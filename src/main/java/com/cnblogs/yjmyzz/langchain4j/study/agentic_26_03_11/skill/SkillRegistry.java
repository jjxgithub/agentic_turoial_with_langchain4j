package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Skill 列表：注册与发现。编排完成后据此匹配应由哪个 Agent 处理。
 */
public class SkillRegistry {

    private final List<Skill> skills = new ArrayList<>();

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

    /**
     * 按关键词匹配：返回第一个命中的 skill；若多个命中可后续改为打分或 LLM 选择。
     */
    public Optional<Skill> findMatch(String executableQuestion) {
        if (executableQuestion == null || executableQuestion.isBlank()) return Optional.empty();
        return skills.stream().filter(s -> s.matches(executableQuestion)).findFirst();
    }

    public List<Skill> getAll() {
        return List.copyOf(skills);
    }
}
