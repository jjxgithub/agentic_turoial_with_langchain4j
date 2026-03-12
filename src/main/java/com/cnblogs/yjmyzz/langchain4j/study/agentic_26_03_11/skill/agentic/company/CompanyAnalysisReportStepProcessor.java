package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepProcessor;
import dev.langchain4j.agentic.scope.AgenticScope;
import org.springframework.stereotype.Component;

/**
 * 公司官网与简析 — 第 2 步前处理：用第 1 步结果填充「公司调研报告」Prompt 模板，供 AI 按模板生成报告。
 */
@Component
public class CompanyAnalysisReportStepProcessor implements StepProcessor {

    @Override
    public void beforeStep(AgenticScope scope, String stepId, String previousOutputKey) {
        String step1Result = String.valueOf(scope.readState(previousOutputKey, "")).trim();
        if (step1Result.isEmpty()) {
            step1Result = "（上一步未返回官网信息，请基于「未知公司」在报告中注明信息有限并做简要说明。）";
        }
        String prompt = CompanyResearchPromptTemplates.COMPANY_RESEARCH_REPORT
                .replace("${step1Result}", step1Result);
        scope.writeState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, prompt);
    }
}
