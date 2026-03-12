package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgent;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 公司官网与简析 Skill — 第 1 步：根据公司名称调用官网查询工具，输出公司名与官网 URL 的 JSON。
 */
public interface FindOfficialWebsiteAgent extends SubAgent {

    @Agent("根据用户给出的公司名称，调用官网查询工具获取该公司官网 URL；将工具返回的 JSON 整理后直接输出，若无官网则保留 note 说明。")
    @SystemMessage("你只能通过调用「根据公司名称查找官网」工具得到结果。输入是公司名称，输出为 JSON：companyName、officialUrl、note。不要编造 URL。")
    @UserMessage("{{currentStepInput}}")
    @Override
    String execute(@V("currentStepInput") String currentStepInput);
}
