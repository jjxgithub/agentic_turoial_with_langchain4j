package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

/**
 * 多轮对话请求体：会话 ID + 用户当前输入。
 */
public record ChatRequest(String sessionId, String input) {
    public ChatRequest {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (input == null) {
            input = "";
        }
    }
}
