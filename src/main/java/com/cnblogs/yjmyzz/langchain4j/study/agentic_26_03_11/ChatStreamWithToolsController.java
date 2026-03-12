package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 单 Agent + 全量工具 版 REST 入口：与 ChatStream0311Controller 相同的请求/响应格式，但走 ToolAugmentedPipelineService（任务由带工具的 Agent 执行）。
 * <p>
 * 请求体：{ "sessionId": "xxx", "input": "用户输入" }
 * 响应（SSE）：clarification / intent_clear → plan → task_start / task_result / task_end → plan_done 或 error。
 */
@RestController
@RequestMapping("/api/agentic_26_03_11")
public class ChatStreamWithToolsController {

    private static final long SSE_TIMEOUT_MS = 120_000;

    private final ToolAugmentedPipelineService toolAugmentedPipelineService;

    public ChatStreamWithToolsController(ToolAugmentedPipelineService toolAugmentedPipelineService) {
        this.toolAugmentedPipelineService = toolAugmentedPipelineService;
    }

    @PostMapping(value = "/chat-with-tools/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWithTools(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);
        toolAugmentedPipelineService.chat(request.sessionId(), request.input(), emitter);
        return emitter;
    }
}
