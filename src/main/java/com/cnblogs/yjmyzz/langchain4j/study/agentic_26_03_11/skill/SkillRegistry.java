package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Skill 列表：注册与发现。编排完成后通过注入的 {@link SkillMatcher} 匹配应由哪个 Agent 处理。
 * 默认使用 LLM 路由（{@link LlmSkillMatcher}），符合行业通用做法。
 */
public class SkillRegistry {

    private final List<Skill> skills = new ArrayList<>();
    private final SkillMatcher matcher;

    public SkillRegistry(SkillMatcher matcher) {
        this.matcher = matcher != null ? matcher : new KeywordSkillMatcher();
    }

    public SkillRegistry register(Skill skill) {
        if (skill != null && skill.id() != null && !skill.id().isBlank()) {
            skills.add(skill);
        }
        return this;
    }

    public SkillRegistry registerAll(List<Skill> list) {
        if (list != null) list.forEach(this::register);
        return this;
    }

    /** 使用配置的匹配策略（如 LLM 路由）返回命中的 skill。 */
    public Optional<Skill> findMatch(String executableQuestion) {
        return matcher.findMatch(executableQuestion, getAll());
    }

    public List<Skill> getAll() {
        return List.copyOf(skills);
    }
}
