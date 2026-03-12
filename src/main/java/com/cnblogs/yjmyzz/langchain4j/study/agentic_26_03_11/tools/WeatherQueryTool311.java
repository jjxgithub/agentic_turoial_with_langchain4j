package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 天气查询工具（agentic_26_03_11 专用，类名不与 26_03_10 重复）。
 * 供单 Agent + 全量工具模式使用；若无法查询请返回含「无法」的说明。
 */
@Component("weatherQueryTool311")
public class WeatherQueryTool311 {

    @Tool("查询指定城市/地区的天气预报，返回简要结论（是否可能下雨等）。若无法获取请返回包含「无法」的说明。")
    public String queryWeather(@P("城市或地区名") String cityOrRegion) {
        if (cityOrRegion == null || cityOrRegion.isBlank()) {
            return "无法获取天气：未提供城市或地区。";
        }
        // 模拟：实际可接入天气 API
        String lower = cityOrRegion.trim().toLowerCase();
        if (lower.contains("北京")) {
            return "北京市：晴，15-25°C，当前无降水预报，今日不可能下雨。";
        }
        if (lower.contains("上海")) {
            return "上海市：多云转阴，18-26°C，傍晚可能有小雨。";
        }
        return String.format("%s：晴转多云，20-28°C，湿度适中。若需实时数据请接入天气 API，当前为模拟结论。", cityOrRegion.trim());
    }
}
