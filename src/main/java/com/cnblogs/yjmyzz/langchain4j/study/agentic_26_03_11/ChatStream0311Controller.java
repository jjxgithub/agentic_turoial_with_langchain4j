package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通用 5 步流水线 REST 入口：澄清 + 补全 + 拆分 + agentic 执行，SSE 推送。
 * <p>
 * 请求体：{ "sessionId": "xxx", "input": "用户输入" }
 * 响应：clarification / intent_clear → plan → task_start / task_result / task_end → plan_done 或 error。
 */
@RestController
@RequestMapping("/api/agentic_26_03_11/chat")
public class ChatStream0311Controller {

    private static final long SSE_TIMEOUT_MS = 120_000;

    private final GenericPipelineService pipelineService;

    public ChatStream0311Controller(GenericPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);
        pipelineService.chat(request.sessionId(), request.input(), emitter);
        return emitter;
    }
}
