package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

/**
 * Skill 内单步声明：步骤 id、名称、对应的 SubAgent 标识；可选的前/后处理（数据组装、查库、调接口等）。
 * 用于通用编排，与具体业务解耦。
 */
public record StepDef(
        String id,
        String name,
        String agentId,
        String preProcessorId,
        String postProcessorId
) {
    /** 仅步骤 + Agent，无前/后处理。 */
    public StepDef(String id, String name, String agentId) {
        this(id, name, agentId, null, null);
    }

    public StepDef {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("step id required");
        if (agentId == null || agentId.isBlank()) throw new IllegalArgumentException("agentId required");
        if (name == null) name = id;
        if (preProcessorId == null) preProcessorId = "";
        if (postProcessorId == null) postProcessorId = "";
    }
}
