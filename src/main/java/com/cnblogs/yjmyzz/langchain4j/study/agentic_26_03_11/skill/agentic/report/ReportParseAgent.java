package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgent;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 报表查询 skill — 第 4 步：报表解析。
 * 输入由 ReportParseStepProcessor.beforeStep 组装（含当前时间、用户身份、字段池等与上一步对齐结果），本 Agent 仅输出最终报表 JSON。
 */
public interface ReportParseAgent extends SubAgent {

    @Agent("严格按指令输出 JSON，不要 Markdown 包裹或解释")
    @SystemMessage("仅输出合法 JSON，包含 fieldList、sumList、countList、groupList、order、filterList、usdExchangeRate、amountList。不要任何解释或多余文字。")
    @UserMessage("{{currentStepInput}}")
    @Override
    String execute(@V("currentStepInput") String currentStepInput);
}
