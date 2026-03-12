package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgent;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 公司官网与简析 Skill — 第 2 步：按「公司调研报告」Prompt 模板，基于上一步的公司名与官网信息生成结构化报告。
 */
public interface CompanyAnalysisReportAgent extends SubAgent {

    @Agent("请严格按照下方「报告要求」与「信息来源」生成公司调研报告，输出为 Markdown，章节完整、表述客观。")
    @SystemMessage("只输出报告正文（Markdown），不要 JSON、不要解释模板本身。信息不足的章节可注明「信息有限」并简要推断。")
    @UserMessage("{{currentStepInput}}")
    @Override
    String execute(@V("currentStepInput") String currentStepInput);
}
