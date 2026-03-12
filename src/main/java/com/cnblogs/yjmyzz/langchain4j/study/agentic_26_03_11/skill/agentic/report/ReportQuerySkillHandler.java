package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

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
            new StepDef("semantic_parse", "语义解析", "semantic_parse", ReportAgenticConfig.PROCESSOR_SEMANTIC, ReportAgenticConfig.PROCESSOR_SEMANTIC),
            new StepDef("intent_extract", "意图提取", "intent_extract", ReportAgenticConfig.PROCESSOR_INTENT, ReportAgenticConfig.PROCESSOR_INTENT),
            new StepDef("align", "对齐", "align", ReportAgenticConfig.PROCESSOR_ALIGN, ReportAgenticConfig.PROCESSOR_ALIGN),
            new StepDef("report_parse", "报表解析", "report_parse", ReportAgenticConfig.PROCESSOR_REPORT_PARSE, ReportAgenticConfig.PROCESSOR_REPORT_PARSE)
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
