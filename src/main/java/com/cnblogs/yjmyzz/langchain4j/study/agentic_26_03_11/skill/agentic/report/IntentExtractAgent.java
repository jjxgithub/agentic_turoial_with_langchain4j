package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgent;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 报表查询 skill — 第 2 步：意图提取。
 * 输入由 IntentExtractStepProcessor.beforeStep 组装，本 Agent 仅输出 JSON（queryType/aggregations/filters 等）。
 */
public interface IntentExtractAgent extends SubAgent {

    @Agent("严格按指令输出 JSON，不要 Markdown 包裹或解释")
    @SystemMessage("仅输出合法 JSON，包含 queryType、aggregations、groupBy、sort、page、filters、entitiesMapping。不要任何解释或多余文字。")
    @UserMessage("{{currentStepInput}}")
    @Override
    String execute(@V("currentStepInput") String currentStepInput);
}
