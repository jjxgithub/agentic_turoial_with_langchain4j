package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.PlanSchema;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SkillWorkflowRunner;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepDef;
import java.util.List;

/**
 * 公司官网查找 Skill：单步，根据公司名称查找官网（调用官网查询 Tool）。
 * 步骤可外置为 skills/steps/company_find_website.json，未配置则用 {@link #defaultSteps()}。
 */
public class CompanyFindWebsiteSkillHandler implements SkillHandler {

    private final SkillWorkflowRunner workflowRunner;
    private final List<StepDef> steps;

    public CompanyFindWebsiteSkillHandler(SkillWorkflowRunner workflowRunner, List<StepDef> steps) {
        this.workflowRunner = workflowRunner;
        this.steps = steps != null && !steps.isEmpty() ? steps : defaultSteps();
    }

    public static List<StepDef> defaultSteps() {
        return List.of(
                new StepDef(
                        Agentic311Constants.CompanyAnalysis.AGENT_FIND_OFFICIAL_WEBSITE,
                        "官网查找",
                        Agentic311Constants.CompanyAnalysis.AGENT_FIND_OFFICIAL_WEBSITE,
                        Agentic311Constants.CompanyAnalysis.PROCESSOR_FIND_WEBSITE,
                        Agentic311Constants.CompanyAnalysis.PROCESSOR_FIND_WEBSITE,
                        List.of(Agentic311Constants.ToolIds.COMPANY_OFFICIAL_WEBSITE_SEARCH)
                )
        );
    }

    @Override
    public String handle(String executableQuestion, PlanSchema plan) {
        return workflowRunner.run(steps, executableQuestion);
    }
}
