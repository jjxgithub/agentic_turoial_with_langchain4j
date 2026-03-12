package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 通用 Skill 子工作流执行器：根据步骤列表 + SubAgent 注册表，用 langchain4j-agentic 的 sequence 编排并执行。
 * 步骤间通过 AgenticScope 传参：currentStepInput ← 上一步结果或 skill_input。
 * 每步可选前/后处理（{@link StepProcessor}）；每步可选异常捕获开关（beforeStep/Agent/afterStep），捕获时写约定错误到 scope 并继续。
 */
public class SkillWorkflowRunner {

    private static final Logger log = LoggerFactory.getLogger(SkillWorkflowRunner.class);

    private final ChatModel chatModel;
    private final SubAgentRegistry subAgentRegistry;
    private final StepProcessorRegistry stepProcessorRegistry;
    private final SubAgentInstanceRegistry instanceRegistry;
    private final ToolRegistry toolRegistry;
    private final ExecutorService timeoutExecutor;

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry) {
        this(chatModel, subAgentRegistry, null, null, null, null);
    }

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry, StepProcessorRegistry stepProcessorRegistry) {
        this(chatModel, subAgentRegistry, stepProcessorRegistry, null, null, null);
    }

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry, StepProcessorRegistry stepProcessorRegistry, SubAgentInstanceRegistry instanceRegistry) {
        this(chatModel, subAgentRegistry, stepProcessorRegistry, instanceRegistry, null, null);
    }

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry, StepProcessorRegistry stepProcessorRegistry, SubAgentInstanceRegistry instanceRegistry, ToolRegistry toolRegistry) {
        this(chatModel, subAgentRegistry, stepProcessorRegistry, instanceRegistry, toolRegistry, null);
    }

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry, StepProcessorRegistry stepProcessorRegistry, SubAgentInstanceRegistry instanceRegistry, ToolRegistry toolRegistry, ExecutorService timeoutExecutor) {
        this.chatModel = chatModel;
        this.subAgentRegistry = subAgentRegistry;
        this.stepProcessorRegistry = stepProcessorRegistry != null ? stepProcessorRegistry : new StepProcessorRegistry();
        this.instanceRegistry = instanceRegistry;
        this.toolRegistry = toolRegistry;
        this.timeoutExecutor = timeoutExecutor;
    }

    /**
     * 根据步骤列表构建 agentic sequence，执行后返回最后一步在 scope 中的结果。无 workflow 超时。
     *
     * @param steps      步骤列表（顺序执行）
     * @param skillInput 入口输入（如当前 task 的 question）
     * @return 最后一步的输出；无步骤或执行异常时返回空字符串
     */
    public String run(List<StepDef> steps, String skillInput) {
        return run(steps, skillInput, StepDef.NO_TIMEOUT_MS);
    }

    /**
     * 同上，可指定整条 workflow 的超时（毫秒）。{@link StepDef#NO_TIMEOUT_MS}（-1）表示不超时。
     *
     * @param workflowTimeoutMs 整条工作流最大执行时间（毫秒），&lt;= 0 或 -1 表示不限制
     */
    public String run(List<StepDef> steps, String skillInput, long workflowTimeoutMs) {
        if (steps == null || steps.isEmpty()) {
            return skillInput != null ? skillInput : "";
        }
        int inputLen = skillInput != null ? skillInput.length() : 0;
        if (log.isDebugEnabled()) {
            log.debug("[SkillWorkflow] run start steps={} inputLen={} workflowTimeoutMs={}", steps.size(), inputLen, workflowTimeoutMs <= 0 ? -1 : workflowTimeoutMs);
        }
        UntypedAgent workflow = buildSequence(steps);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put(Agentic311Constants.ScopeKeys.SKILL_INPUT, skillInput != null ? skillInput : "");
        try {
            Object result = (workflowTimeoutMs > 0)
                    ? runWithOptionalTimeout(workflowTimeoutMs, () -> workflow.invoke(input))
                    : workflow.invoke(input);
            String resultStr = result instanceof String ? (String) result : (result != null ? result.toString() : "");
            if (ErrorPayloads.isErrorPayload(resultStr)) {
                return ErrorPayloads.toFriendlyMessage(resultStr);
            }
            String out = resultStr != null ? resultStr : "";
            if (log.isDebugEnabled()) {
                log.debug("[SkillWorkflow] run done resultLen={}", out.length());
            }
            return out;
        } catch (StepValidationException e) {
            throw e;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("[SkillWorkflow] invoke failed, steps={} inputLen={}, error={}", steps.size(), inputLen, msg, e);
            return "";
        }
    }

    /**
     * 构建 sequence：每步 = [前处理可选] + 写 currentStepInput（或由前处理写入） + SubAgent + [后处理可选]。
     * 当 StepDef 的 catchBeforeStepError/catchAgentError/catchAfterStepError 为 true 时，对应阶段异常被捕获、写约定错误到 scope 并继续下一步。
     * 构建前会校验每步的 agentId、preProcessorId、postProcessorId 已在对应注册表中注册，否则抛出 {@link StepValidationException}。
     */
    UntypedAgent buildSequence(List<StepDef> steps) {
        validateSteps(steps);
        List<Object> subAgents = new ArrayList<>();
        String firstInputKey = Agentic311Constants.ScopeKeys.SKILL_INPUT;
        for (StepDef step : steps) {
            if (log.isDebugEnabled()) {
                log.debug("[SkillWorkflow] building step stepId={}", step.id());
            }
            String stepResultKey = stepResultKey(step.id());
            Class<? extends SubAgent> agentClass = subAgentRegistry.getAgentClass(step.agentId());
            final String prevKey = firstInputKey;

            // 前处理（可选捕获）
            if (hasProcessor(step.preProcessorId())) {
                StepProcessor pre = stepProcessorRegistry.get(step.preProcessorId());
                if (pre != null) {
                    subAgents.add(step.catchBeforeStepError()
                            ? agentActionCatch(step.id(), prevKey, scope -> pre.beforeStep(scope, step.id(), prevKey))
                            : AgenticServices.agentAction(scope -> pre.beforeStep(scope, step.id(), prevKey)));
                } else {
                    subAgents.add(copyToCurrentStepInput(prevKey));
                }
            } else {
                subAgents.add(copyToCurrentStepInput(prevKey));
            }

            // SubAgent：若本步配置了 toolIds 则用 agentBuilder + tools（不沿用预建实例）；否则可选实例+捕获或 agentBuilder
            List<Object> stepTools = (toolRegistry != null && step.toolIds() != null && !step.toolIds().isEmpty())
                    ? toolRegistry.getTools(step.toolIds()) : List.of();
            boolean useTools = !stepTools.isEmpty();

            long stepTimeoutMs = step.stepTimeoutMs() > 0 ? step.stepTimeoutMs() : StepDef.NO_TIMEOUT_MS;
            SubAgent instance = (!useTools && instanceRegistry != null) ? instanceRegistry.get(step.agentId()) : null;
            if (!useTools && step.catchAgentError() && instance != null) {
                subAgents.add(agentActionInvokeAgent(step.id(), stepResultKey, step.agentRetryCount(), stepTimeoutMs, instance));
            } else {
                var builder = AgenticServices.agentBuilder(agentClass)
                        .chatModel(chatModel)
                        .outputKey(stepResultKey);
                if (useTools) {
                    for (Object tool : stepTools) {
                        builder = builder.tools(tool);
                    }
                }
                // builder.build() 返回实现 SubAgent 的 JDK 代理，按 SubAgent.execute(input) 调用并统一重试/超时
                SubAgent builtAgent = builder.build();
                subAgents.add(agentActionInvokeAgent(step.id(), stepResultKey, step.agentRetryCount(), stepTimeoutMs, builtAgent));
            }

            // 后处理（可选捕获）
            if (hasProcessor(step.postProcessorId())) {
                StepProcessor post = stepProcessorRegistry.get(step.postProcessorId());
                if (post != null) {
                    subAgents.add(step.catchAfterStepError()
                            ? agentActionCatchAfter(step.id(), stepResultKey, scope -> post.afterStep(scope, step.id(), stepResultKey))
                            : AgenticServices.agentAction(scope -> post.afterStep(scope, step.id(), stepResultKey)));
                }
            }

            firstInputKey = stepResultKey;
        }
        String lastResultKey = stepResultKey(steps.get(steps.size() - 1).id());
        return AgenticServices.sequenceBuilder()
                .subAgents(subAgents.toArray())
                .output(scope -> String.valueOf(scope.readState(lastResultKey, "")))
                .build();
    }

    /**
     * 前处理阶段：捕获异常时写错误 payload 到 currentStepInput，后续步骤可据此降级。
     */
    private Object agentActionCatch(String stepId, String prevKey, AgentScopeAction action) {
        return AgenticServices.agentAction(scope -> {
            try {
                action.run(scope);
            } catch (Exception e) {
                log.warn("[SkillWorkflow] beforeStep failed, catch and continue stepId={} error={}", stepId, e.getMessage(), e);
                scope.writeState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, formatErrorPayload(stepId, e));
            }
        });
    }

    /**
     * 后处理阶段：捕获异常时仅打日志，不覆盖 stepResultKey（保留 Agent 输出）。
     */
    private Object agentActionCatchAfter(String stepId, String stepResultKey, AgentScopeAction action) {
        return AgenticServices.agentAction(scope -> {
            try {
                action.run(scope);
            } catch (Exception e) {
                log.warn("[SkillWorkflow] afterStep failed, catch and continue stepId={} resultKey={} error={}", stepId, stepResultKey, e.getMessage(), e);
            }
        });
    }

    /**
     * 在 agentAction 内调用 SubAgent 实例并捕获异常，写入 stepResultKey。
     * 当 agentRetryCount &gt; 0 时，失败后会重试至多 agentRetryCount 次（总尝试次数 = 1 + agentRetryCount）。
     * stepTimeoutMs &gt; 0 时在本步执行上施加超时（可选）。
     */
    private Object agentActionInvokeAgent(String stepId, String stepResultKey, int agentRetryCount, long stepTimeoutMs, SubAgent instance) {
        return AgenticServices.agentAction(scope -> {
            String input = String.valueOf(scope.readState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, ""));
            if (ErrorPayloads.isErrorPayload(input)) {
                scope.writeState(stepResultKey, input);
                return;
            }
            int maxAttempts = 1 + Math.max(0, agentRetryCount);
            Exception lastException = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    String result = runWithOptionalTimeout(stepTimeoutMs, () -> instance.execute(input));
                    scope.writeState(stepResultKey, result != null ? result : "");
                    return;
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < maxAttempts) {
                        log.debug("[SkillWorkflow] step agent attempt {}/{} failed, retrying stepId={} error={}", attempt, maxAttempts, stepId, e.getMessage());
                    } else {
                        String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                        log.warn("[SkillWorkflow] step agent failed after {} attempt(s) stepId={} error={}", maxAttempts, stepId, errMsg, e);
                    }
                }
            }
            scope.writeState(stepResultKey, formatErrorPayload(stepId, lastException != null ? lastException : new RuntimeException("unknown")));
        });
    }

    /**
     * 在 agentAction 内调用 builder 产出的 UntypedAgent，统一应用重试与可选超时；失败时按 catchAgentError 写入错误 payload。
     */
    private Object agentActionInvokeBuiltAgent(String stepId, String stepResultKey, int agentRetryCount, long stepTimeoutMs, boolean catchAgentError, UntypedAgent builtAgent) {
        return AgenticServices.agentAction(scope -> {
            String input = String.valueOf(scope.readState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, ""));
            if (ErrorPayloads.isErrorPayload(input)) {
                scope.writeState(stepResultKey, input);
                return;
            }
            Map<String, Object> stateMap = scope.state();
            if (stateMap == null) {
                scope.writeState(stepResultKey, formatErrorPayload(stepId, new IllegalStateException("scope.state() is null")));
                return;
            }
            int maxAttempts = 1 + Math.max(0, agentRetryCount);
            Exception lastException = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    runWithOptionalTimeout(stepTimeoutMs, () -> {
                        builtAgent.invoke(stateMap);
                        return null;
                    });
                    return;
                } catch (Exception e) {
                    lastException = e;
                    if (attempt < maxAttempts) {
                        log.debug("[SkillWorkflow] step builtAgent attempt {}/{} failed, retrying stepId={} error={}", attempt, maxAttempts, stepId, e.getMessage());
                    } else {
                        String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                        log.warn("[SkillWorkflow] step builtAgent failed after {} attempt(s) stepId={} error={}", maxAttempts, stepId, errMsg, e);
                    }
                }
            }
            if (catchAgentError) {
                scope.writeState(stepResultKey, formatErrorPayload(stepId, lastException != null ? lastException : new RuntimeException("unknown")));
            } else {
                throw lastException != null ? (lastException instanceof RuntimeException re ? re : new RuntimeException(lastException)) : new RuntimeException("unknown");
            }
        });
    }

    /** 无超时时直接执行；stepTimeoutMs &gt; 0 时在单独线程中执行并等待，超时抛异常。使用可注入的 timeoutExecutor，未注入时用静态兜底（daemon，不阻塞 JVM 退出）。 */
    private <T> T runWithOptionalTimeout(long stepTimeoutMs, java.util.concurrent.Callable<T> callable) throws Exception {
        if (stepTimeoutMs <= 0) {
            return callable.call();
        }
        ExecutorService executor = timeoutExecutor != null ? timeoutExecutor : FALLBACK_TIMEOUT_EXECUTOR;
        java.util.concurrent.Future<T> future = executor.submit(callable);
        try {
            return future.get(stepTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Step timeout after " + stepTimeoutMs + " ms", e);
        }
    }

    /** 未注入 timeoutExecutor 时使用的静态兜底线程池（daemon），不参与应用关闭时的 shutdown。 */
    private static final ExecutorService FALLBACK_TIMEOUT_EXECUTOR =
            java.util.concurrent.Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "skill-step-timeout-fallback");
                t.setDaemon(true);
                return t;
            });

    private static String formatErrorPayload(String stepId, Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        msg = msg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
        return String.format(Agentic311Constants.ScopeKeys.ERROR_PAYLOAD_TEMPLATE, stepId, msg);
    }

    @FunctionalInterface
    private interface AgentScopeAction {
        void run(AgenticScope scope);
    }

    /**
     * 校验步骤列表：每步的 agentId 必须在 SubAgentRegistry 中注册；
     * 若 preProcessorId/postProcessorId 非空，则必须在 StepProcessorRegistry 中注册。
     * 不通过则抛出 {@link StepValidationException}，便于配置错误快速失败。
     */
    private void validateSteps(List<StepDef> steps) {
        if (steps == null) return;
        for (StepDef step : steps) {
            String stepId = step.id();
            if (!subAgentRegistry.has(step.agentId())) {
                throw new StepValidationException(stepId, "agentId='" + step.agentId() + "' not registered in SubAgentRegistry");
            }
            if (hasProcessor(step.preProcessorId()) && !stepProcessorRegistry.has(step.preProcessorId())) {
                throw new StepValidationException(stepId, "preProcessorId='" + step.preProcessorId() + "' not registered in StepProcessorRegistry");
            }
            if (hasProcessor(step.postProcessorId()) && !stepProcessorRegistry.has(step.postProcessorId())) {
                throw new StepValidationException(stepId, "postProcessorId='" + step.postProcessorId() + "' not registered in StepProcessorRegistry");
            }
        }
    }

    private boolean hasProcessor(String processorId) {
        return processorId != null && !processorId.isBlank();
    }

    private static Object copyToCurrentStepInput(String prevKey) {
        return AgenticServices.agentAction(scope -> {
            String prev = String.valueOf(scope.readState(prevKey, ""));
            scope.writeState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, prev);
        });
    }

    private static String stepResultKey(String stepId) {
        return Agentic311Constants.ScopeKeys.STEP_RESULT_PREFIX + stepId + Agentic311Constants.ScopeKeys.STEP_RESULT_SUFFIX;
    }
}
