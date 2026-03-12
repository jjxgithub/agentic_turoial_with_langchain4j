package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.util.Collections;
import java.util.List;

/**
 * Skill 内单步声明：步骤 id、名称、对应的 SubAgent 标识；可选的前/后处理；可选的异常捕获开关；可选的本步 Tool 列表（按 id 配置）。
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
        List<String> toolIds
) {
    /** 仅步骤 + Agent，无前/后处理、不捕获异常、无 tools。 */
    public StepDef(String id, String name, String agentId) {
        this(id, name, agentId, null, null, false, false, false, null);
    }

    /** 步骤 + Agent + 前/后处理 id，不捕获异常、无 tools。 */
    public StepDef(String id, String name, String agentId, String preProcessorId, String postProcessorId) {
        this(id, name, agentId, preProcessorId, postProcessorId, false, false, false, null);
    }

    /** 步骤 + Agent + 前/后处理 id + 本步使用的 tool 的 id 列表（从 {@link ToolRegistry} 解析）。 */
    public StepDef(String id, String name, String agentId, String preProcessorId, String postProcessorId, List<String> toolIds) {
        this(id, name, agentId, preProcessorId, postProcessorId, false, false, false, toolIds);
    }

    public StepDef {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("step id required");
        if (agentId == null || agentId.isBlank()) throw new IllegalArgumentException("agentId required");
        if (name == null) name = id;
        if (preProcessorId == null) preProcessorId = "";
        if (postProcessorId == null) postProcessorId = "";
        if (toolIds == null) toolIds = Collections.emptyList();
        else toolIds = List.copyOf(toolIds);
    }
}
