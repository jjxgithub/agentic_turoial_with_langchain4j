package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.util.Collections;
import java.util.List;

/**
 * Skill 内单步声明：步骤 id、名称、对应的 SubAgent 标识；可选的前/后处理；可选的异常捕获开关；
 * 可选的 Agent 重试次数；可选的本步 Tool 列表（按 id 或 groupId 配置）；可选的本步超时（毫秒，-1 表示不超时）。
 * 用于通用编排，与具体业务解耦。
 */
public record StepDef(
        String id,
        String name,
        String agentId,
        String preProcessorId,
        String postProcessorId,
        boolean catchBeforeStepError,
        boolean catchAgentError,
        boolean catchAfterStepError,
        int agentRetryCount,
        List<String> toolIds,
        long stepTimeoutMs
) {
    /** 不超时时的规范值，用于 stepTimeoutMs 与 workflow 超时。 */
    public static final long NO_TIMEOUT_MS = -1L;

    /** 仅步骤 + Agent，无前/后处理、不捕获异常、无 tools、重试 0 次、无超时。 */
    public StepDef(String id, String name, String agentId) {
        this(id, name, agentId, null, null, false, false, false, 0, null, NO_TIMEOUT_MS);
    }

    /** 步骤 + Agent + 前/后处理 id，不捕获异常、无 tools、重试 0 次、无超时。 */
    public StepDef(String id, String name, String agentId, String preProcessorId, String postProcessorId) {
        this(id, name, agentId, preProcessorId, postProcessorId, false, false, false, 0, null, NO_TIMEOUT_MS);
    }

    /** 步骤 + Agent + 前/后处理 id + 本步使用的 tool id 列表（可为单个 tool id 或 {@link ToolRegistry} 的 groupId）。 */
    public StepDef(String id, String name, String agentId, String preProcessorId, String postProcessorId, List<String> toolIds) {
        this(id, name, agentId, preProcessorId, postProcessorId, false, false, false, 0, toolIds, NO_TIMEOUT_MS);
    }

    /** 步骤 + Agent + 前/后处理 id + toolIds + Agent 重试次数。 */
    public StepDef(String id, String name, String agentId, String preProcessorId, String postProcessorId, int agentRetryCount, List<String> toolIds) {
        this(id, name, agentId, preProcessorId, postProcessorId, false, false, false, agentRetryCount, toolIds, NO_TIMEOUT_MS);
    }

    public StepDef {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("step id required");
        if (agentId == null || agentId.isBlank()) throw new IllegalArgumentException("agentId required");
        if (name == null) name = id;
        if (preProcessorId == null) preProcessorId = "";
        if (postProcessorId == null) postProcessorId = "";
        if (agentRetryCount < 0) agentRetryCount = 0;
        if (toolIds == null) toolIds = Collections.emptyList();
        else toolIds = List.copyOf(toolIds);
        if (stepTimeoutMs < -1) stepTimeoutMs = NO_TIMEOUT_MS;
    }
}
