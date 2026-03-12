package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 Skill 子工作流执行器：根据步骤列表 + SubAgent 注册表，用 langchain4j-agentic 的 sequence 编排并执行。
 * 步骤间通过 AgenticScope 传参：currentStepInput ← 上一步结果或 skill_input。
 * 每步可选前/后处理（{@link StepProcessor}）；每步可选异常捕获开关（beforeStep/Agent/afterStep），捕获时写约定错误到 scope 并继续。
 */
public class SkillWorkflowRunner {

    private static final Logger log = LoggerFactory.getLogger(SkillWorkflowRunner.class);

    /** 入口输入在 scope 中的 key，invoke 时传入。 */
    public static final String SKILL_INPUT = "skill_input";
    /** 每步从 scope 读取的输入 key，由本 runner 在每步前写入（或由 StepProcessor.beforeStep 写入）。 */
    public static final String CURRENT_STEP_INPUT = "currentStepInput";
    /** 每步结果在 scope 中的 key 前缀，完整 key = STEP_RESULT_PREFIX + stepId。 */
    public static final String STEP_RESULT_PREFIX = "step_";
    public static final String STEP_RESULT_SUFFIX = "_result";

    /** 捕获异常时写入 scope 的约定格式，下游可解析 error/stepId/message 做降级。 */
    public static final String ERROR_PAYLOAD_TEMPLATE = "{\"error\":true,\"stepId\":\"%s\",\"message\":\"%s\"}";

    private final ChatModel chatModel;
    private final SubAgentRegistry subAgentRegistry;
    private final StepProcessorRegistry stepProcessorRegistry;
    private final SubAgentInstanceRegistry instanceRegistry;

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry) {
        this(chatModel, subAgentRegistry, null, null);
    }

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry, StepProcessorRegistry stepProcessorRegistry) {
        this(chatModel, subAgentRegistry, stepProcessorRegistry, null);
    }

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry, StepProcessorRegistry stepProcessorRegistry, SubAgentInstanceRegistry instanceRegistry) {
        this.chatModel = chatModel;
        this.subAgentRegistry = subAgentRegistry;
        this.stepProcessorRegistry = stepProcessorRegistry != null ? stepProcessorRegistry : new StepProcessorRegistry();
        this.instanceRegistry = instanceRegistry;
    }

    /**
     * 根据步骤列表构建 agentic sequence，执行后返回最后一步在 scope 中的结果。
     *
     * @param steps 步骤列表（顺序执行）
     * @param skillInput 入口输入（如当前 task 的 question）
     * @return 最后一步的输出；无步骤或执行异常时返回空字符串
     */
    public String run(List<StepDef> steps, String skillInput) {
        if (steps == null || steps.isEmpty()) {
            return skillInput != null ? skillInput : "";
        }
        UntypedAgent workflow = buildSequence(steps);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put(SKILL_INPUT, skillInput != null ? skillInput : "");
        try {
            Object result = workflow.invoke(input);
            if (result instanceof String) return (String) result;
            return result != null ? result.toString() : "";
        } catch (Exception e) {
            log.warn("Skill workflow invoke failed", e);
            return "";
        }
    }

    /**
     * 构建 sequence：每步 = [前处理可选] + 写 currentStepInput（或由前处理写入） + SubAgent + [后处理可选]。
     * 当 StepDef 的 catchBeforeStepError/catchAgentError/catchAfterStepError 为 true 时，对应阶段异常被捕获、写约定错误到 scope 并继续下一步。
     */
    UntypedAgent buildSequence(List<StepDef> steps) {
        List<Object> subAgents = new ArrayList<>();
        String firstInputKey = SKILL_INPUT;
        for (StepDef step : steps) {
            String stepResultKey = stepResultKey(step.id());
            Class<? extends SubAgent> agentClass = subAgentRegistry.getAgentClass(step.agentId());
            if (agentClass == null) {
                log.warn("No SubAgent registered for agentId={}, stepId={}", step.agentId(), step.id());
                subAgents.add(AgenticServices.agentAction(scope -> scope.writeState(stepResultKey, "")));
                firstInputKey = stepResultKey;
                continue;
            }
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

            // SubAgent：可选通过实例调用并捕获，否则走 agentBuilder（异常会中断整链）
            SubAgent instance = (instanceRegistry != null) ? instanceRegistry.get(step.agentId()) : null;
            if (step.catchAgentError() && instance != null) {
                subAgents.add(agentActionInvokeAgent(step.id(), stepResultKey, instance));
            } else {
                subAgents.add(AgenticServices.agentBuilder(agentClass)
                        .chatModel(chatModel)
                        .outputKey(stepResultKey)
                        .build());
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

    /** 前处理阶段：捕获异常时写错误 payload 到 currentStepInput，后续步骤可据此降级。 */
    private Object agentActionCatch(String stepId, String prevKey, AgentScopeAction action) {
        return AgenticServices.agentAction(scope -> {
            try {
                action.run(scope);
            } catch (Exception e) {
                log.warn("Step beforeStep failed, catch and continue: stepId={}", stepId, e);
                scope.writeState(CURRENT_STEP_INPUT, formatErrorPayload(stepId, e));
            }
        });
    }

    /** 后处理阶段：捕获异常时仅打日志，不覆盖 stepResultKey（保留 Agent 输出）。 */
    private Object agentActionCatchAfter(String stepId, String stepResultKey, AgentScopeAction action) {
        return AgenticServices.agentAction(scope -> {
            try {
                action.run(scope);
            } catch (Exception e) {
                log.warn("Step afterStep failed, catch and continue: stepId={} resultKey={}", stepId, stepResultKey, e);
            }
        });
    }

    /** 在 agentAction 内调用 SubAgent 实例并捕获异常，写入 stepResultKey。 */
    private Object agentActionInvokeAgent(String stepId, String stepResultKey, SubAgent instance) {
        return AgenticServices.agentAction(scope -> {
            String input = String.valueOf(scope.readState(CURRENT_STEP_INPUT, ""));
            try {
                String result = instance.execute(input);
                scope.writeState(stepResultKey, result != null ? result : "");
            } catch (Exception e) {
                log.warn("Step agent failed, catch and continue: stepId={}", stepId, e);
                scope.writeState(stepResultKey, formatErrorPayload(stepId, e));
            }
        });
    }

    private static String formatErrorPayload(String stepId, Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        msg = msg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
        return String.format(ERROR_PAYLOAD_TEMPLATE, stepId, msg);
    }

    @FunctionalInterface
    private interface AgentScopeAction {
        void run(AgenticScope scope);
    }

    private boolean hasProcessor(String processorId) {
        return processorId != null && !processorId.isBlank();
    }

    private static Object copyToCurrentStepInput(String prevKey) {
        return AgenticServices.agentAction(scope -> {
            String prev = String.valueOf(scope.readState(prevKey, ""));
            scope.writeState(CURRENT_STEP_INPUT, prev);
        });
    }

    private static String stepResultKey(String stepId) {
        return STEP_RESULT_PREFIX + stepId + STEP_RESULT_SUFFIX;
    }
}
