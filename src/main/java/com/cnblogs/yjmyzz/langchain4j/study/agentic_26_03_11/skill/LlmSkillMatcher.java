package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 使用 LLM 做技能匹配（行业常见做法）：将技能列表与用户输入交给 LLM，由 LLM 返回最匹配的 skill id。
 */
public class LlmSkillMatcher implements SkillMatcher {

    private static final Logger log = LoggerFactory.getLogger(LlmSkillMatcher.class);
    private static final String NONE = "none";

    private final SkillRouter router;

    public LlmSkillMatcher(SkillRouter router) {
        this.router = router;
    }

    @Override
    public Optional<Skill> findMatch(String executableQuestion, List<Skill> skills) {
        if (executableQuestion == null || executableQuestion.isBlank() || skills == null || skills.isEmpty()) {
            return Optional.empty();
        }
        String skillsDescription = formatSkills(skills);
        String raw;
        try {
            raw = router.selectSkillId(skillsDescription, executableQuestion);
        } catch (Exception e) {
            log.warn("Skill router call failed, treat as no match", e);
            return Optional.empty();
        }
        String id = raw == null ? "" : raw.trim().toLowerCase();
        if (id.isBlank() || NONE.equals(id)) {
            return Optional.empty();
        }
        return skills.stream()
                .filter(s -> s.id() != null && id.equals(s.id().trim().toLowerCase()))
                .findFirst();
    }

    private static String formatSkills(List<Skill> skills) {
        StringBuilder sb = new StringBuilder();
        for (Skill s : skills) {
            sb.append("id: ").append(s.id()).append("\n");
            sb.append("name: ").append(s.name() != null ? s.name() : "").append("\n");
            sb.append("description: ").append(s.description() != null ? s.description() : "").append("\n\n");
        }
        return sb.toString();
    }
}
