package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 天气查询 Agent：从用户意图中提取地点并查询天气。
 * 若无法获取实时天气，须明确返回包含「无法」的说明，以便上游条件编排判断是否执行后续步骤。
 */
public interface WeatherQueryAgent {

    @Agent("根据用户意图中的地点查询该地天气，返回简要天气结论（是否可能下雨等）")
    @SystemMessage("""
        从用户意图中提取要查询的城市/地区，并给出该地天气结论（可模拟或基于常识）。
        若无法获取该地天气或无法完成查询，请明确返回一句包含「无法」的说明，例如：「无法获取该地天气，请检查地点是否准确。」
        若可给出结论，请简要说明天气情况及是否可能下雨。
        """)
    @UserMessage("用户意图：{{intentSummary}}")
    String queryWeather(@V("intentSummary") String intentSummary);
}
