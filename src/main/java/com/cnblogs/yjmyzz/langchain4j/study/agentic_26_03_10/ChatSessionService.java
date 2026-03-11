package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一对话服务：先判断意图是否清晰，不清晰则发澄清；清晰则用意图摘要做子问题拆分与编排。
 * 按 sessionId 维护对话记忆。
 */
@Service
public class ChatSessionService {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

    private static final int MAX_MEMORY_MESSAGES = 20;
    private static final String NEED_CLARIFICATION = "NEED_CLARIFICATION";
    private static final String ANSWER = "ANSWER";

    private final Map<String, MessageWindowChatMemory> sessionMemories = new ConcurrentHashMap<>();
    private final MultiTurnAssistant assistant;
    private final AgenticWorkflowService agenticWorkflowService;

    public ChatSessionService(ChatModel chatModel, AgenticWorkflowService agenticWorkflowService) {
        ChatMemoryProvider provider = memoryId -> sessionMemories.computeIfAbsent(
                String.valueOf(memoryId),
                k -> MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES)
        );
        this.assistant = AiServices.builder(MultiTurnAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(provider)
                .build();
        this.agenticWorkflowService = agenticWorkflowService;
    }

    /**
     * 统一入口：先判断意图是否清晰。不清晰则推送 clarification 并结束；清晰则用意图摘要做编排，推送 plan / task_* / plan_done。
     *
     * @param sessionId 会话 ID（用于记忆）
     * @param userInput 用户当前输入
     * @param emitter   SSE 发射器
     */
    public void chat(String sessionId, String userInput, SseEmitter emitter) {
        try {
            String response = assistant.chat(sessionId, userInput);
            if (response == null || response.isBlank()) {
                sendEvent(emitter, "clarification", "请再具体说明一下您的需求。");
                sendEvent(emitter, "done", null);
                emitter.complete();
                return;
            }
            String firstLine = response.lines().findFirst().orElse("").strip();
            String content = response.contains("\n")
                    ? response.substring(response.indexOf('\n') + 1).trim()
                    : "";

            if (NEED_CLARIFICATION.equalsIgnoreCase(firstLine)) {
                sendEvent(emitter, "clarification", content);
                sendEvent(emitter, "done", null);
                emitter.complete();
            } else if (ANSWER.equalsIgnoreCase(firstLine)) {
                sendEvent(emitter, "intent_clear", content);
                agenticWorkflowService.runWeatherThenConditionalEmailStream(sessionId, content, emitter);
            } else {
                sendEvent(emitter, "intent_clear", response);
                agenticWorkflowService.runWeatherThenConditionalEmailStream(sessionId, response, emitter);
            }
        } catch (Exception e) {
            log.warn("chat error, sessionId={}", sessionId, e);
            try {
                sendEvent(emitter, "error", e.getMessage());
            } catch (IOException ignored) {
            }
            emitter.completeWithError(e);
        }
    }

    private static void sendEvent(SseEmitter emitter, String eventType, String data) throws IOException {
        if (data != null) {
            emitter.send(SseEmitter.event().name(eventType).data(data));
        } else {
            emitter.send(SseEmitter.event().name(eventType));
        }
    }
}
