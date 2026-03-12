package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * 单步前后的数据组装钩子：在 SubAgent（LLM）执行前/后执行自定义逻辑（如查库、调接口、拼装数据）。
 * <p>
 * 前处理：从 scope 读上一步结果（previousOutputKey），做组装后必须将本步输入写入 {@link com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants.ScopeKeys#CURRENT_STEP_INPUT}。
 * 若上一步失败，Runner 会写入约定错误 payload；可在 beforeStep 内用 {@link ErrorPayloads#isErrorPayload(String)} 识别后直接透传或写入友好提示，避免把错误 JSON 当正常输入拼进 prompt。
 * <p>
 * 后处理：从 scope 读本步结果（stepResultKey），做解析/调接口/落库等，可写回 stepResultKey 或其它 key（下一步的输入默认仍为 stepResultKey）。
 */
public interface StepProcessor {

    /**
     * 在 SubAgent 执行前调用。可从 scope 读 previousOutputKey，查库/调接口后把本步输入写入 currentStepInput。
     *
     * @param scope             当前 scope，可 readState/writeState
     * @param stepId            当前步骤 id
     * @param previousOutputKey 上一步结果在 scope 中的 key（首步为 skill_input）
     */
    default void beforeStep(AgenticScope scope, String stepId, String previousOutputKey) {
    }

    /**
     * 在 SubAgent 执行后调用。可从 scope 读 stepResultKey（本步 LLM 输出），做解析/调接口/落库后写回 scope。
     *
     * @param scope        当前 scope
     * @param stepId       当前步骤 id
     * @param stepResultKey 本步结果在 scope 中的 key（step_{id}_result）
     */
    default void afterStep(AgenticScope scope, String stepId, String stepResultKey) {
    }
}
