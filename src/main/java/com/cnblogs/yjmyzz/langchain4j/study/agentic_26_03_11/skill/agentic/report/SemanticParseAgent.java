package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgent;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 报表查询 skill — 第 1 步：语义解析。
 * 输入由 SemanticParseStepProcessor.beforeStep 组装（含模块信息与用户输入），本 Agent 仅输出 JSON。
 */
public interface SemanticParseAgent extends SubAgent {

    @Agent("严格按指令输出 JSON，不要 Markdown 包裹或解释")
    @SystemMessage("仅输出合法 JSON，格式为 {\"link\":\"and|or\",\"moduleCodeList\":[...]}。不要任何解释、代码块或多余文字。")
    @UserMessage("{{currentStepInput}}")
    @Override
    String execute(@V("currentStepInput") String currentStepInput);
}
