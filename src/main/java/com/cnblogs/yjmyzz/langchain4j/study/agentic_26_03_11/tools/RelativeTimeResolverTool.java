package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 相对时间解析 Tool：将自然语言时间描述解析为具体日期范围（startDate,endDate），供报表解析等步骤使用。
 * 支持：今天/昨天、近N天、近N月/周、本月/本周、上周/上星期、上周X/本周X/周X、去年/前年、本季度/上季度、今年/去年/前年+第N季度。
 * 在 Skill StepDef 中通过 toolIds 配置为 "relativeTimeResolver" 使用。
 * 维护：新增时间说法时在 {@link #resolvers} 中追加一条即可。
 */
@Component("relativeTimeResolverTool311")
public class RelativeTimeResolverTool {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Pattern NEAR_DAYS = Pattern.compile("(?:近|最\\s*近)\\s*(\\d+)\\s*天");
    private static final Pattern YEAR_QUARTER = Pattern.compile("(今|去|前|大前)\\s*年\\s*(?:第?([一二三四1234])\\s*季度?)?");
    private static final Pattern THIS_LAST_QUARTER = Pattern.compile("(本|上上|上)\\s*季度?");
    private static final Pattern WEEK_WITH_DAY = Pattern.compile("(上|本|这)\\s*(?:周|星期)\\s*([一二三四五六日天])");
    private static final Pattern WEEKDAY_ONLY = Pattern.compile("(?:^|\\s)周([一二三四五六日天])|(?:^|\\s)星期([一二三四五六日天])");

    /** 解析器链：按顺序尝试，先匹配先返回。新增规则在此列表末尾追加。 */
    private static final List<TimeResolver> RESOLVERS = List.of(
            RelativeTimeResolverTool::resolveToday,
            RelativeTimeResolverTool::resolveYesterday,
            RelativeTimeResolverTool::resolveDayBeforeYesterday,
            RelativeTimeResolverTool::resolveWeekWithDay,
            RelativeTimeResolverTool::resolveLastWeek,
            RelativeTimeResolverTool::resolveWeekdayOnly,
            RelativeTimeResolverTool::resolveYearQuarter,
            RelativeTimeResolverTool::resolveThisLastQuarter,
            RelativeTimeResolverTool::resolveNearDays,
            RelativeTimeResolverTool::resolveNearMonths,
            RelativeTimeResolverTool::resolveNearWeeks,
            RelativeTimeResolverTool::resolveThisMonth,
            RelativeTimeResolverTool::resolveThisWeek
    );

    @Tool("将相对时间描述解析为日期范围。输入例如：今天、昨天、近三天、上周、上周一、本周三、周一、去年、本季度。返回 JSON：{\"startDate\":\"yyyy-MM-dd\",\"endDate\":\"yyyy-MM-dd\"}；无法解析时返回说明。")
    public String resolveRelativeTime(@P("相对时间描述，如：今天、昨天、近三天、去年、前年、本季度、去年第三季度") String relativeTimeDesc) {
        if (relativeTimeDesc == null || relativeTimeDesc.isBlank()) {
            return "{\"error\":\"未提供相对时间描述\"}";
        }
        String s = relativeTimeDesc.trim();
        LocalDate today = LocalDate.now();

        for (TimeResolver r : RESOLVERS) {
            Optional<DateRange> range = r.resolve(s, today);
            if (range.isPresent()) {
                return String.format("{\"startDate\":\"%s\",\"endDate\":\"%s\"}",
                        range.get().start.format(FMT), range.get().end.format(FMT));
            }
        }
        return "{\"error\":\"无法解析相对时间: " + s + "\"}";
    }

    // ---------- 解析器实现（顺序与 RESOLVERS 一致，需保持“更具体”的在前） ----------

    private static Optional<DateRange> resolveToday(String s, LocalDate today) {
        return match(s, "今\\s*天", "今日") ? Optional.of(new DateRange(today, today)) : Optional.empty();
    }

    private static Optional<DateRange> resolveYesterday(String s, LocalDate today) {
        if (!match(s, "昨\\s*天", "昨日")) return Optional.empty();
        LocalDate d = today.minusDays(1);
        return Optional.of(new DateRange(d, d));
    }

    private static Optional<DateRange> resolveDayBeforeYesterday(String s, LocalDate today) {
        if (!match(s, "前\\s*天", "前天")) return Optional.empty();
        LocalDate d = today.minusDays(2);
        return Optional.of(new DateRange(d, d));
    }

    private static Optional<DateRange> resolveWeekWithDay(String s, LocalDate today) {
        Matcher m = WEEK_WITH_DAY.matcher(s);
        if (!m.find()) return Optional.empty();
        int dow = parseDayOfWeek(m.group(2));
        LocalDate mon = thisWeekMonday(today);
        LocalDate day = "上".equals(m.group(1)) ? mon.minusWeeks(1).plusDays(dow - 1) : mon.plusDays(dow - 1);
        return Optional.of(new DateRange(day, day));
    }

    private static Optional<DateRange> resolveLastWeek(String s, LocalDate today) {
        if (!match(s, "上\\s*周", "上\\s*星期")) return Optional.empty();
        LocalDate mon = thisWeekMonday(today).minusWeeks(1);
        return Optional.of(new DateRange(mon, mon.plusDays(6)));
    }

    private static Optional<DateRange> resolveWeekdayOnly(String s, LocalDate today) {
        Matcher m = WEEKDAY_ONLY.matcher(s);
        if (!m.find()) return Optional.empty();
        String c = m.group(1) != null ? m.group(1) : m.group(2);
        LocalDate day = thisWeekMonday(today).plusDays(parseDayOfWeek(c) - 1);
        return Optional.of(new DateRange(day, day));
    }

    private static Optional<DateRange> resolveYearQuarter(String s, LocalDate today) {
        Matcher m = YEAR_QUARTER.matcher(s);
        if (!m.find()) return Optional.empty();
        int yOffset = switch (m.group(1).charAt(0)) {
            case '今' -> 0;
            case '去' -> -1;
            case '前' -> -2;
            default -> -3;
        };
        int year = today.getYear() + yOffset;
        String qStr = m.group(2);
        if (qStr != null && !qStr.isBlank()) {
            int q = parseQuarter(qStr.trim());
            return Optional.of(new DateRange(quarterStart(year, q), quarterEnd(year, q)));
        }
        return Optional.of(new DateRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    }

    private static Optional<DateRange> resolveThisLastQuarter(String s, LocalDate today) {
        Matcher m = THIS_LAST_QUARTER.matcher(s);
        if (!m.find()) return Optional.empty();
        int shift = "本".equals(m.group(1)) ? 0 : "上上".equals(m.group(1)) ? -2 : -1;
        int year = today.getYear();
        int q = (today.getMonthValue() - 1) / 3 + 1;
        int targetQ = q + shift;
        while (targetQ < 1) { targetQ += 4; year--; }
        while (targetQ > 4) { targetQ -= 4; year++; }
        return Optional.of(new DateRange(quarterStart(year, targetQ), quarterEnd(year, targetQ)));
    }

    private static Optional<DateRange> resolveNearDays(String s, LocalDate today) {
        Matcher m = NEAR_DAYS.matcher(s);
        if (!m.find()) return Optional.empty();
        int n = Math.max(1, Integer.parseInt(m.group(1)));
        return Optional.of(new DateRange(today.minusDays(n - 1), today));
    }

    private static Optional<DateRange> resolveNearMonths(String s, LocalDate today) {
        LocalDate start;
        if (match(s, "近\\s*一\\s*个?月", "最\\s*近\\s*一\\s*个?月", "最近一个月", "近一个月")) {
            start = today.minusMonths(1);
        } else if (match(s, "近\\s*两\\s*个?月", "最近两个月")) {
            start = today.minusMonths(2);
        } else if (match(s, "近\\s*三\\s*个?月", "最近三个月")) {
            start = today.minusMonths(3);
        } else {
            return Optional.empty();
        }
        return Optional.of(new DateRange(start, today));
    }

    private static Optional<DateRange> resolveNearWeeks(String s, LocalDate today) {
        LocalDate start;
        if (match(s, "最\\s*近\\s*一\\s*周", "近\\s*一\\s*周", "最近一周", "近一周", "最近7天", "近7天")) {
            start = today.minusWeeks(1);
        } else if (match(s, "最\\s*近\\s*两\\s*周", "近\\s*两\\s*周", "最近14天")) {
            start = today.minusWeeks(2);
        } else {
            return Optional.empty();
        }
        return Optional.of(new DateRange(start, today));
    }

    private static Optional<DateRange> resolveThisMonth(String s, LocalDate today) {
        if (!match(s, "本\\s*月", "本月")) return Optional.empty();
        return Optional.of(new DateRange(today.withDayOfMonth(1), today));
    }

    private static Optional<DateRange> resolveThisWeek(String s, LocalDate today) {
        if (!match(s, "本\\s*周", "本周")) return Optional.empty();
        LocalDate start = thisWeekMonday(today);
        return Optional.of(new DateRange(start, today));
    }

    // ---------- 内部类型与工具方法 ----------

    private record DateRange(LocalDate start, LocalDate end) {}

    @FunctionalInterface
    private interface TimeResolver {
        Optional<DateRange> resolve(String input, LocalDate today);
    }

    private static LocalDate thisWeekMonday(LocalDate today) {
        LocalDate mon = today.with(DayOfWeek.MONDAY);
        return mon.isAfter(today) ? mon.minusWeeks(1) : mon;
    }

    private static LocalDate quarterStart(int year, int quarter) {
        int month = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, month, 1);
    }

    private static LocalDate quarterEnd(int year, int quarter) {
        return switch (quarter) {
            case 1 -> LocalDate.of(year, 3, 31);
            case 2 -> LocalDate.of(year, 6, 30);
            case 3 -> LocalDate.of(year, 9, 30);
            default -> LocalDate.of(year, 12, 31);
        };
    }

    private static int parseQuarter(String s) {
        if (s == null || s.isEmpty()) return 1;
        char c = s.charAt(0);
        if (c >= '1' && c <= '4') return c - '0';
        return switch (c) {
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            default -> 1;
        };
    }

    private static int parseDayOfWeek(String c) {
        if (c == null || c.isEmpty()) return 1;
        return switch (c.charAt(0)) {
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '日', '天' -> 7;
            default -> 1;
        };
    }

    private static boolean match(String input, String... patterns) {
        for (String p : patterns) {
            if (Pattern.compile(p).matcher(input).find()) return true;
        }
        return false;
    }
}
