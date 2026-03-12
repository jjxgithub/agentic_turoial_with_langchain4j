package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import java.util.List;

/**
 * 技能项：用于编排后做「发现」，命中后由对应 Agent（SkillHandler）处理。
 * 可与 langchain-skill 规范对齐：name、description、匹配条件（如 keywords）等。
 */
public record Skill(
        String id,
        String name,
        String description,
        List<String> keywords,
        SkillHandler handler
) {
    /** 关键词匹配：用户输入是否包含任一 keyword（忽略大小写）。 */
    public boolean matches(String userInput) {
        if (userInput == null || keywords == null || keywords.isEmpty()) return false;
        String lower = userInput.toLowerCase();
        return keywords.stream().anyMatch(k -> k != null && !k.isBlank() && lower.contains(k.toLowerCase()));
    }
}
