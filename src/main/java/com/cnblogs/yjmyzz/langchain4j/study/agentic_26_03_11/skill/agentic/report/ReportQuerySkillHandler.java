package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.PlanSchema;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SkillWorkflowRunner;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepDef;

import java.util.List;

/**
 * 报表查询 skill 的 Handler：内部 4 步（语义解析 → 意图提取 → 对齐 → 报表解析）由通用 SkillWorkflowRunner + agentic 执行。
 * 由 ReportAgenticConfig 中 @Bean 注册，不在此类上使用 @Component 避免与 @Bean 重复定义。
 */
public class ReportQuerySkillHandler implements SkillHandler {

    private static final List<StepDef> REPORT_STEPS = List.of(
            new StepDef(Agentic311Constants.Report.AGENT_SEMANTIC_PARSE, "语义解析", Agentic311Constants.Report.AGENT_SEMANTIC_PARSE, Agentic311Constants.Report.PROCESSOR_SEMANTIC, Agentic311Constants.Report.PROCESSOR_SEMANTIC),
            new StepDef(Agentic311Constants.Report.AGENT_INTENT_EXTRACT, "意图提取", Agentic311Constants.Report.AGENT_INTENT_EXTRACT, Agentic311Constants.Report.PROCESSOR_INTENT, Agentic311Constants.Report.PROCESSOR_INTENT),
            new StepDef(Agentic311Constants.Report.AGENT_ALIGN, "对齐", Agentic311Constants.Report.AGENT_ALIGN, Agentic311Constants.Report.PROCESSOR_ALIGN, Agentic311Constants.Report.PROCESSOR_ALIGN),
            new StepDef(Agentic311Constants.Report.AGENT_REPORT_PARSE, "报表解析", Agentic311Constants.Report.AGENT_REPORT_PARSE, Agentic311Constants.Report.PROCESSOR_REPORT_PARSE, Agentic311Constants.Report.PROCESSOR_REPORT_PARSE, List.of(Agentic311Constants.ToolIds.RELATIVE_TIME_RESOLVER))
    );

    private final SkillWorkflowRunner workflowRunner;

    public ReportQuerySkillHandler(SkillWorkflowRunner reportWorkflowRunner) {
        this.workflowRunner = reportWorkflowRunner;
    }

    @Override
    public String handle(String executableQuestion, PlanSchema plan) {
        return workflowRunner.run(REPORT_STEPS, executableQuestion);
    }
}
