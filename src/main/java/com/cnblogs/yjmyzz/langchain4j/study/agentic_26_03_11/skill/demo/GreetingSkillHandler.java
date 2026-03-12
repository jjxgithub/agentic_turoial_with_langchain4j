package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.PlanSchema;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillHandler;
import org.springframework.stereotype.Component;

/**
 * Demo：打招呼 Skill 的 Agent，内部两步——(1) 识别语言 (2) 生成该语言的问候。
 * 对应「报表查询」Agent 内四步的简化版演示。
 */
@Component
public class GreetingSkillHandler implements SkillHandler {

    private final DetectLanguageStep detectLanguageStep;
    private final GenerateGreetingStep generateGreetingStep;

    public GreetingSkillHandler(DetectLanguageStep detectLanguageStep, GenerateGreetingStep generateGreetingStep) {
        this.detectLanguageStep = detectLanguageStep;
        this.generateGreetingStep = generateGreetingStep;
    }

    @Override
    public String handle(String executableQuestion, PlanSchema plan) {
        String lang = detectLanguageStep.detect(executableQuestion != null ? executableQuestion : "");
        if (lang == null || lang.isBlank()) lang = "中文";
        return generateGreetingStep.generate(executableQuestion != null ? executableQuestion : "", lang);
    }
}
