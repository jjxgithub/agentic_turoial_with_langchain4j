package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.util.Collections;
import java.util.List;

/**
 * Skill 内单步声明：步骤 id、名称、对应的 SubAgent 标识；可选的前/后处理；可选的异常捕获开关；
 * 可选的 Agent 重试次数（仅在使用预注册实例时生效）；可选的本步 Tool 列表（按 id 或 groupId 配置）。
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
        List<String> toolIds
) {
    /** 仅步骤 + Agent，无前/后处理、不捕获异常、无 tools、重试 0 次。 */
    public StepDef(String id, String name, String agentId) {
        this(id, name, agentId, null, null, false, false, false, 0, null);
    }

    /** 步骤 + Agent + 前/后处理 id，不捕获异常、无 tools、重试 0 次。 */
    public StepDef(String id, String name, String agentId, String preProcessorId, String postProcessorId) {
        this(id, name, agentId, preProcessorId, postProcessorId, false, false, false, 0, null);
    }

    /** 步骤 + Agent + 前/后处理 id + 本步使用的 tool id 列表（可为单个 tool id 或 {@link ToolRegistry} 的 groupId）。 */
    public StepDef(String id, String name, String agentId, String preProcessorId, String postProcessorId, List<String> toolIds) {
        this(id, name, agentId, preProcessorId, postProcessorId, false, false, false, 0, toolIds);
    }

    /** 步骤 + Agent + 前/后处理 id + toolIds + Agent 重试次数（仅在使用预注册实例并开启 catchAgentError 时生效）。 */
    public StepDef(String id, String name, String agentId, String preProcessorId, String postProcessorId, int agentRetryCount, List<String> toolIds) {
        this(id, name, agentId, preProcessorId, postProcessorId, false, false, false, agentRetryCount, toolIds);
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
    }
}
