package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

/**
 * Tool 组定义：对 LLM 暴露为一个逻辑 Tool，内部按优先级顺序执行成员并在失败/无结果时 fallback。
 */
public record GroupDef(
        String groupId,
        String displayName,
        String description
) {
    public GroupDef {
        if (groupId == null) groupId = "";
        if (displayName == null) displayName = groupId;
        if (description == null) description = "";
    }
}
