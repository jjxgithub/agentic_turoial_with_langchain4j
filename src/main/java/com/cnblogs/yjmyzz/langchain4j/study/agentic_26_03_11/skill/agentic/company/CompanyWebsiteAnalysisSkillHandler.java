package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.PlanSchema;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SkillWorkflowRunner;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepDef;
import java.util.List;

/**
 * 公司网站分析 Skill：单步，根据上文结果（公司名、官网等）生成调研报告。
 * 输入由流水线注入（含依赖任务结果），本步用公司调研模板生成报告。
 * 步骤可外置为 skills/steps/company_website_analysis.json，未配置则用 {@link #defaultSteps()}。
 */
public class CompanyWebsiteAnalysisSkillHandler implements SkillHandler {

    private final SkillWorkflowRunner workflowRunner;
    private final List<StepDef> steps;

    public CompanyWebsiteAnalysisSkillHandler(SkillWorkflowRunner workflowRunner, List<StepDef> steps) {
        this.workflowRunner = workflowRunner;
        this.steps = steps != null && !steps.isEmpty() ? steps : defaultSteps();
    }

    public static List<StepDef> defaultSteps() {
        return List.of(
                new StepDef(
                        Agentic311Constants.CompanyAnalysis.AGENT_ANALYSIS_REPORT,
                        "分析报告",
                        Agentic311Constants.CompanyAnalysis.AGENT_ANALYSIS_REPORT,
                        Agentic311Constants.CompanyAnalysis.PROCESSOR_ANALYSIS_REPORT,
                        Agentic311Constants.CompanyAnalysis.PROCESSOR_ANALYSIS_REPORT
                )
        );
    }

    @Override
    public String handle(String executableQuestion, PlanSchema plan) {
        return workflowRunner.run(steps, executableQuestion);
    }
}
