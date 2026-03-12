package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import java.util.List;
import java.util.Optional;

/**
 * 按关键词包含匹配：用户输入包含某 skill 的任一 keyword 即命中，返回第一个命中的 skill。
 * 可用于回退或测试，默认推荐使用 {@link LlmSkillMatcher}。
 */
public class KeywordSkillMatcher implements SkillMatcher {

    @Override
    public Optional<Skill> findMatch(String executableQuestion, List<Skill> skills) {
        if (executableQuestion == null || executableQuestion.isBlank() || skills == null) {
            return Optional.empty();
        }
        return skills.stream()
                .filter(s -> s != null && s.matches(executableQuestion))
                .findFirst();
    }
}
