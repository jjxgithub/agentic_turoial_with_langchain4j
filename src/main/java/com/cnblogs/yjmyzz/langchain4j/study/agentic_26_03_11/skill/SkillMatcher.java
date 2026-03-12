package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import java.util.List;
import java.util.Optional;

/**
 * Skill 匹配策略：根据用户可执行问题从候选 skills 中选出最匹配的一项（或 none）。
 * 常见实现：关键词包含（KeywordSkillMatcher）、LLM 路由（LlmSkillMatcher）。
 */
public interface SkillMatcher {

    /**
     * 从候选列表中选出与用户输入最匹配的一个 skill；无匹配时返回 empty。
     */
    Optional<Skill> findMatch(String executableQuestion, List<Skill> skills);
}
