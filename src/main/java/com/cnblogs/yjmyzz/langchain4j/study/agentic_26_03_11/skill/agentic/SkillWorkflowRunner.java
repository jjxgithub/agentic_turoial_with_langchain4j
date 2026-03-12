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
 * 每步可选前/后处理（{@link StepProcessor}）：前处理做数据组装（查库、调接口）后写 currentStepInput，后处理对 LLM 结果做解析/落库等。
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

    private final ChatModel chatModel;
    private final SubAgentRegistry subAgentRegistry;
    private final StepProcessorRegistry stepProcessorRegistry;

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry) {
        this(chatModel, subAgentRegistry, null);
    }

    public SkillWorkflowRunner(ChatModel chatModel, SubAgentRegistry subAgentRegistry, StepProcessorRegistry stepProcessorRegistry) {
        this.chatModel = chatModel;
        this.subAgentRegistry = subAgentRegistry;
        this.stepProcessorRegistry = stepProcessorRegistry != null ? stepProcessorRegistry : new StepProcessorRegistry();
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
     * 前处理：StepProcessor.beforeStep 可从 scope 读上一步、查库/调接口后写 currentStepInput；未配置则默认把上一步结果拷贝到 currentStepInput。
     * 后处理：StepProcessor.afterStep 可从 scope 读本步结果、解析/调接口/落库后写回 stepResultKey。
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

            // 前处理：自定义 StepProcessor 或默认「上一步结果 → currentStepInput」
            if (hasProcessor(step.preProcessorId())) {
                StepProcessor pre = stepProcessorRegistry.get(step.preProcessorId());
                if (pre != null) {
                    subAgents.add(AgenticServices.agentAction(scope -> pre.beforeStep(scope, step.id(), prevKey)));
                } else {
                    subAgents.add(copyToCurrentStepInput(prevKey));
                }
            } else {
                subAgents.add(copyToCurrentStepInput(prevKey));
            }

            subAgents.add(AgenticServices.agentBuilder(agentClass)
                    .chatModel(chatModel)
                    .outputKey(stepResultKey)
                    .build());

            // 后处理：对本步结果做解析/调接口/落库等
            if (hasProcessor(step.postProcessorId())) {
                StepProcessor post = stepProcessorRegistry.get(step.postProcessorId());
                if (post != null) {
                    subAgents.add(AgenticServices.agentAction(scope -> post.afterStep(scope, step.id(), stepResultKey)));
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
