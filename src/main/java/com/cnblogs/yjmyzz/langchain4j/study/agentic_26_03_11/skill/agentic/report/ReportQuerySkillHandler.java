package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.PlanSchema;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SkillWorkflowRunner;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepDef;

import java.util.List;

/**
 * 报表查询 skill 的 Handler：内部步骤由 {@link com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepDefLoader} 从
 * classpath:skills/steps/report_query.json 加载；若未配置则使用 {@link #defaultSteps()}。
 * 由 ReportAgenticConfig 中 @Bean 注册，不在此类上使用 @Component 避免与 @Bean 重复定义。
 */
public class ReportQuerySkillHandler implements SkillHandler {

    private final SkillWorkflowRunner workflowRunner;
    private final List<StepDef> steps;

    public ReportQuerySkillHandler(SkillWorkflowRunner workflowRunner, List<StepDef> steps) {
        this.workflowRunner = workflowRunner;
        this.steps = steps != null && !steps.isEmpty() ? steps : defaultSteps();
    }

    /** 外置配置未加载时的默认步骤（与原 REPORT_STEPS 一致）。 */
    public static List<StepDef> defaultSteps() {
        return List.of(
                new StepDef(Agentic311Constants.Report.AGENT_SEMANTIC_PARSE, "语义解析", Agentic311Constants.Report.AGENT_SEMANTIC_PARSE, Agentic311Constants.Report.PROCESSOR_SEMANTIC, Agentic311Constants.Report.PROCESSOR_SEMANTIC),
                new StepDef(Agentic311Constants.Report.AGENT_INTENT_EXTRACT, "意图提取", Agentic311Constants.Report.AGENT_INTENT_EXTRACT, Agentic311Constants.Report.PROCESSOR_INTENT, Agentic311Constants.Report.PROCESSOR_INTENT),
                new StepDef(Agentic311Constants.Report.AGENT_ALIGN, "对齐", Agentic311Constants.Report.AGENT_ALIGN, Agentic311Constants.Report.PROCESSOR_ALIGN, Agentic311Constants.Report.PROCESSOR_ALIGN),
                new StepDef(Agentic311Constants.Report.AGENT_REPORT_PARSE, "报表解析", Agentic311Constants.Report.AGENT_REPORT_PARSE, Agentic311Constants.Report.PROCESSOR_REPORT_PARSE, Agentic311Constants.Report.PROCESSOR_REPORT_PARSE, List.of(Agentic311Constants.ToolIds.RELATIVE_TIME_RESOLVER))
        );
    }

    @Override
    public String handle(String executableQuestion, PlanSchema plan) {
        return workflowRunner.run(steps, executableQuestion);
    }
}
