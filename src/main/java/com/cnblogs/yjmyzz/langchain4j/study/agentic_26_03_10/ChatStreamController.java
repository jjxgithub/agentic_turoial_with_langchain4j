package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 统一对话 REST 接口（仅一个入口）：先判断意图是否清晰，不清晰则澄清，清晰则自动做子问题拆分与编排。
 * <p>
 * 客户端发送 sessionId + input；服务端根据记忆判断「补充 vs 新问题」与是否清晰，
 * 若需澄清则推送 clarification + done，若清晰则推送 intent_clear + plan + task_* + plan_done。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatStreamController {

    private static final long SSE_TIMEOUT_MS = 120_000;

    private final ChatSessionService chatSessionService;

    public ChatStreamController(ChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    /**
     * 统一流式接口（SSE）：一个入口同时支持「澄清」与「编排」。
     * <p>
     * 请求体：{ "sessionId": "xxx", "input": "用户输入" }
     * 响应（text/event-stream）：
     * - 意图不清晰时：clarification（澄清问题）→ done
     * - 意图清晰时：intent_clear（意图摘要）→ plan → task_start / task_result / task_end → plan_done
     * - 出错：error
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);

        chatSessionService.chat(request.sessionId(), request.input(), emitter);
        return emitter;
    }
}
