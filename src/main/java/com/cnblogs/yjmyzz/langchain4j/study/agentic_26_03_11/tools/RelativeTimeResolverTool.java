package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 相对时间解析 Tool：将「近一个月」「最近一周」等自然语言解析为具体日期范围（startDate,endDate），供报表解析等步骤使用。
 * 在 Skill StepDef 中通过 toolIds 配置为 "relativeTimeResolver" 使用。
 */
@Component("relativeTimeResolverTool311")
public class RelativeTimeResolverTool {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Tool("将相对时间描述解析为日期范围。输入例如：近一个月、最近一周、最近7天、本月、本周。返回 JSON 格式：{\"startDate\":\"yyyy-MM-dd\",\"endDate\":\"yyyy-MM-dd\"}；无法解析时返回说明。")
    public String resolveRelativeTime(@P("相对时间描述，如：近一个月、最近一周、本周、本月") String relativeTimeDesc) {
        if (relativeTimeDesc == null || relativeTimeDesc.isBlank()) {
            return "{\"error\":\"未提供相对时间描述\"}";
        }
        String s = relativeTimeDesc.trim();
        LocalDate end = LocalDate.now();
        LocalDate start;

        if (match(s, "近\\s*一\\s*个?月", "最\\s*近\\s*一\\s*个?月", "最近一个月", "近一个月")) {
            start = end.minusMonths(1);
        } else if (match(s, "近\\s*两\\s*个?月", "最近两个月")) {
            start = end.minusMonths(2);
        } else if (match(s, "近\\s*三\\s*个?月", "最近三个月")) {
            start = end.minusMonths(3);
        } else if (match(s, "最\\s*近\\s*一\\s*周", "近\\s*一\\s*周", "最近一周", "近一周", "最近7天", "近7天")) {
            start = end.minusWeeks(1);
        } else if (match(s, "最\\s*近\\s*两\\s*周", "近\\s*两\\s*周", "最近14天")) {
            start = end.minusWeeks(2);
        } else if (match(s, "本\\s*月", "本月")) {
            start = end.withDayOfMonth(1);
        } else if (match(s, "本\\s*周", "本周")) {
            start = end.with(java.time.DayOfWeek.MONDAY);
            if (start.isAfter(end)) start = start.minusWeeks(1);
        } else {
            return "{\"error\":\"无法解析相对时间: " + s + "\"}";
        }

        return String.format("{\"startDate\":\"%s\",\"endDate\":\"%s\"}",
                start.format(FMT), end.format(FMT));
    }

    private static boolean match(String input, String... patterns) {
        for (String p : patterns) {
            if (Pattern.compile(p).matcher(input).find()) return true;
        }
        return false;
    }
}
