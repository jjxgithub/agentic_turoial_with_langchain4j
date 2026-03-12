package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgent;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 报表查询 skill — 第 3 步：对齐。
 * 输入由 AlignStepProcessor.beforeStep 组装（含候选字段池与上一步意图 JSON），本 Agent 仅输出对齐后的 JSON。
 */
public interface AlignAgent extends SubAgent {

    @Agent("严格按指令输出 JSON，不要 Markdown 包裹或解释")
    @SystemMessage("仅输出合法 JSON，将 field_n 替换为候选字段池中的标准名称，含 includeMyself。不要任何解释或多余文字。")
    @UserMessage("{{currentStepInput}}")
    @Override
    String execute(@V("currentStepInput") String currentStepInput);
}
