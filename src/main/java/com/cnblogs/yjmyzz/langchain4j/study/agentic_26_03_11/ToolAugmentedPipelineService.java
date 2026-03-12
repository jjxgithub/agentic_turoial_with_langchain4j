package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 单 Agent + 全量工具版流水线：与 GenericPipelineService 相同的 5 步，但执行阶段使用 PlanInterpreterWithTools（任务由带工具的 Agent 执行）。
 */
@Service
public class ToolAugmentedPipelineService {

    private static final Logger log = LoggerFactory.getLogger(ToolAugmentedPipelineService.class);
    private static final String NEED_CLARIFICATION = "NEED_CLARIFICATION";
    private static final String PROCEED = "PROCEED";

    private final ClarificationAnalyzer clarificationAnalyzer;
    private final QuestionReformulator questionReformulator;
    private final PlanPlanner planPlanner;
    private final PlanInterpreterWithTools planInterpreterWithTools;
    private final ResultSummaryAgent311 resultSummaryAgent311;
    private final ObjectMapper objectMapper;

    public ToolAugmentedPipelineService(
            ClarificationAnalyzer clarificationAnalyzer,
            QuestionReformulator questionReformulator,
            PlanPlanner planPlanner,
            PlanInterpreterWithTools planInterpreterWithTools,
            ResultSummaryAgent311 resultSummaryAgent311,
            ObjectMapper objectMapper) {
        this.clarificationAnalyzer = clarificationAnalyzer;
        this.questionReformulator = questionReformulator;
        this.planPlanner = planPlanner;
        this.planInterpreterWithTools = planInterpreterWithTools;
        this.resultSummaryAgent311 = resultSummaryAgent311;
        this.objectMapper = objectMapper;
    }

    public void chat(String sessionId, String userInput, SseEmitter emitter) {
        try {
            String analysis = clarificationAnalyzer.analyze(sessionId, userInput);
            if (analysis == null || analysis.isBlank()) {
                sendEvent(emitter, "clarification", "请再具体说明一下您的需求。");
                sendEvent(emitter, "done", null);
                emitter.complete();
                return;
            }
            String firstLine = analysis.lines().findFirst().orElse("").strip();
            String content = analysis.contains("\n")
                    ? analysis.substring(analysis.indexOf('\n') + 1).trim()
                    : "";

            if (NEED_CLARIFICATION.equalsIgnoreCase(firstLine)) {
                sendEvent(emitter, "clarification", content);
                sendEvent(emitter, "done", null);
                emitter.complete();
                return;
            }

            String executableQuestion = questionReformulator.reformulate(sessionId, userInput);
            if (executableQuestion == null || executableQuestion.isBlank()) {
                executableQuestion = userInput;
            }
            sendEvent(emitter, "intent_clear", executableQuestion);

            String planJson = planPlanner.plan(executableQuestion);
            PlanSchema plan = planInterpreterWithTools.parsePlan(planJson);
            if (plan.tasks().isEmpty()) {
                sendEvent(emitter, "plan_done", "{\"summary\":\"未拆分子任务\"}");
                emitter.complete();
                return;
            }
            sendEvent(emitter, "plan", objectMapper.writeValueAsString(plan));

            List<TaskSchema> sorted = planInterpreterWithTools.topologicalSort(plan);
            for (TaskSchema t : sorted) {
                sendEvent(emitter, "task_start", t.id());
            }
            Map<String, Object> results = planInterpreterWithTools.execute(plan, executableQuestion);

            if (results.containsKey("supervisorSummary")) {
                sendEvent(emitter, "plan_done", String.valueOf(results.get("supervisorSummary")));
            } else {
                StringBuilder resultsText = new StringBuilder();
                for (TaskSchema t : sorted) {
                    Object r = results.get(t.id());
                    String resultStr = r != null ? r.toString() : "";
                    String taskResultJson = objectMapper.createObjectNode()
                            .put("taskId", t.id())
                            .put("question", t.question())
                            .put("result", resultStr)
                            .toString();
                    sendEvent(emitter, "task_result", taskResultJson);
                    sendEvent(emitter, "task_end", t.id());
                    resultsText.append("- [").append(t.id()).append("] ").append(t.question()).append(" → ").append(resultStr).append("\n");
                }
                String summaryText;
                try {
                    summaryText = resultSummaryAgent311.summarize(executableQuestion, resultsText.toString());
                } catch (Exception e) {
                    log.warn("result summary agent failed, fallback to raw results error={}", e.getMessage(), e);
                    summaryText = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
                }
                sendEvent(emitter, "plan_done", summaryText);
            }
            emitter.complete();
        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("chat error sessionId={} error={}", sessionId, errMsg, e);
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
