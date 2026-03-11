package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 langchain4j-agentic 的顺序 + 条件编排：先查天气，仅当 A 成功且结果含「可能下雨」时才执行发邮件。
 */
@Service
public class AgenticWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(AgenticWorkflowService.class);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    public AgenticWorkflowService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行「天气查询 → 条件发邮件」工作流，并通过 SSE 推送 plan 与 plan_done。
     * 仅当 A 成功且天气结果含「雨」时才会执行 B；若 A 返回「无法」等，B 不执行。
     */
    @SuppressWarnings("unchecked")
    public void runWeatherThenConditionalEmailStream(String sessionId, String intentSummary, SseEmitter emitter) {
        try {
            sendEvent(emitter, "plan",
                    objectMapper.writeValueAsString(Map.of("tasks", List.of(
                            Map.of("id", "A", "question", "查询天气", "dependsOn", List.of()),
                            Map.of("id", "B", "question", "若可能下雨则发邮件（依赖 A 成功且结果含雨）", "dependsOn", List.of("A"))
                    ))));

            Object weatherAgent = AgenticServices.agentBuilder(WeatherQueryAgent.class)
                    .chatModel(chatModel)
                    .outputKey("weatherResult")
                    .build();
            Object emailAgent = AgenticServices.agentBuilder(EmailNotifyAgent.class)
                    .chatModel(chatModel)
                    .outputKey("emailResult")
                    .build();

            UntypedAgent sequence = AgenticServices.sequenceBuilder()
                    .subAgents(weatherAgent, conditionalEmailAgent(emailAgent))
                    .output(scope -> {
                        Map<String, Object> out = new LinkedHashMap<>();
                        out.put("weatherResult", scope.readState("weatherResult", ""));
                        out.put("emailResult", scope.readState("emailResult", ""));
                        return out;
                    })
                    .build();

            Map<String, Object> input = Map.of("intentSummary", intentSummary);
            Object result = sequence.invoke(input);

            if (result instanceof Map) {
                String summary = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString((Map<?, ?>) result);
                sendEvent(emitter, "plan_done", summary);
            } else {
                sendEvent(emitter, "plan_done", String.valueOf(result));
            }
            emitter.complete();
        } catch (Exception e) {
            log.warn("runWeatherThenConditionalEmailStream error, sessionId={}", sessionId, e);
            try {
                sendEvent(emitter, "error", e.getMessage());
            } catch (IOException ignored) {
            }
            emitter.completeWithError(e);
        }
    }

    /** 仅当 weatherResult 存在、不含「无法」且含「雨」时才执行发邮件 Agent。 */
    private static UntypedAgent conditionalEmailAgent(Object emailAgent) {
        return AgenticServices.conditionalBuilder()
                .subAgents(AgenticWorkflowService::needSendEmail, emailAgent)
                .outputKey("emailResult")
                .build();
    }

    private static boolean needSendEmail(AgenticScope scope) {
        String weatherResult = String.valueOf(scope.readState("weatherResult", ""));
        return !weatherResult.contains("无法") && weatherResult.contains("雨");
    }

    private static void sendEvent(SseEmitter emitter, String eventType, String data) throws IOException {
        if (data != null) {
            emitter.send(SseEmitter.event().name(eventType).data(data));
        } else {
            emitter.send(SseEmitter.event().name(eventType));
        }
    }
}
