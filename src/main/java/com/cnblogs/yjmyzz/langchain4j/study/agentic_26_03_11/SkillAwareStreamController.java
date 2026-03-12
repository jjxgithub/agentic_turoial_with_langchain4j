package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 编排后按 skill 发现的流水线入口：补全 → 拆分 → 编排 → skill 发现 → 命中则交对应 Agent（内部多步），否则走通用 plan 执行。
 * <p>
 * POST /api/agentic_26_03_11/skill-aware/stream
 * Body: { "sessionId": "xxx", "input": "用户输入" }
 * Demo：输入含「你好」「hello」等会命中「打招呼」skill，由 GreetingAgent 内部两步（识别语言→生成问候）处理。
 */
@RestController
@RequestMapping("/api/agentic_26_03_11")
public class SkillAwareStreamController {

    private static final long SSE_TIMEOUT_MS = 120_000;

    private final SkillAwarePipelineService skillAwarePipelineService;

    public SkillAwareStreamController(SkillAwarePipelineService skillAwarePipelineService) {
        this.skillAwarePipelineService = skillAwarePipelineService;
    }

    @PostMapping(value = "/skill-aware/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);
        skillAwarePipelineService.chat(
                request.sessionId() != null ? request.sessionId() : "skill-" + System.currentTimeMillis(),
                request.input(),
                emitter
        );
        return emitter;
    }
}
