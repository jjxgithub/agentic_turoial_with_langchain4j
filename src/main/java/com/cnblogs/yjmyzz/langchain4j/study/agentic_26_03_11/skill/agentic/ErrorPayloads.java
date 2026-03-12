package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;

import java.util.regex.Pattern;

/**
 * 框架层对「约定错误 payload」的识别与友好化，与具体 skill 解耦。
 * <p>
 * 当某步异常被捕获时，Runner 会按 {@link Agentic311Constants.ScopeKeys#ERROR_PAYLOAD_TEMPLATE} 写入 scope。
 * 下游可通过 {@link #isErrorPayload(String)} 识别后短路（如不再调 LLM、或直接返回友好提示）。
 */
public final class ErrorPayloads {

    private static final Pattern ERROR_PAYLOAD_PATTERN = Pattern.compile("\\{\\s*\"error\"\\s*:\\s*true\\s*,");

    private ErrorPayloads() {}

    /**
     * 判断字符串是否为约定错误 payload（JSON 且含 "error":true）。
     * 与 {@link Agentic311Constants.ScopeKeys#ERROR_PAYLOAD_TEMPLATE} 格式一致即可识别，不依赖完整解析。
     */
    public static boolean isErrorPayload(String value) {
        if (value == null) return false;
        String s = value.trim();
        return s.startsWith("{") && ERROR_PAYLOAD_PATTERN.matcher(s).find();
    }

    /**
     * 将错误 payload 转为对用户友好的短句，便于 Runner 最终返回或 StepProcessor 降级展示。
     * 若 value 非错误 payload 则返回原值；若解析不出 stepId/message 则返回默认文案。
     */
    public static String toFriendlyMessage(String value) {
        if (value == null || value.isBlank()) return defaultFriendlyMessage();
        if (!isErrorPayload(value)) return value;
        String stepId = extractJsonString(value, "stepId");
        String message = extractJsonString(value, "message");
        if (stepId != null && message != null && !message.isBlank()) {
            return String.format("步骤「%s」执行失败：%s", stepId, message);
        }
        if (message != null && !message.isBlank()) {
            return "执行失败：" + message;
        }
        return defaultFriendlyMessage();
    }

    /** 无法解析时的默认提示，可被上层常量或配置覆盖。 */
    public static String defaultFriendlyMessage() {
        return "某步执行失败，请稍后重试。";
    }

    /** 简单从 JSON 串中取字符串字段（不依赖完整 JSON 库，避免边界依赖）。 */
    static String extractJsonString(String json, String key) {
        if (json == null || key == null) return null;
        String quoted = "\"" + key + "\"";
        int idx = json.indexOf(quoted);
        if (idx < 0) return null;
        idx = json.indexOf(":", idx + quoted.length());
        if (idx < 0) return null;
        idx = json.indexOf("\"", idx + 1);
        if (idx < 0) return null;
        int end = idx + 1;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') { end += 2; continue; }
            if (c == '"') break;
            end++;
        }
        if (end >= json.length()) return null;
        String raw = json.substring(idx + 1, end);
        return raw.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
