package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.PlanSchema;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillHandler;
import org.springframework.stereotype.Component;

/**
 * Demo：告别 Skill 的 Agent，根据用户输入生成一句简短道别（单步）。
 */
@Component
public class FarewellSkillHandler implements SkillHandler {

    private final FarewellReplyStep farewellReplyStep;

    public FarewellSkillHandler(FarewellReplyStep farewellReplyStep) {
        this.farewellReplyStep = farewellReplyStep;
    }

    @Override
    public String handle(String executableQuestion, PlanSchema plan) {
        return farewellReplyStep.reply(executableQuestion != null ? executableQuestion : "");
    }
}
