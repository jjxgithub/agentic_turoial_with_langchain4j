package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 通用 5 步流水线：缺条件分析 → 问题补全 → 子问题拆分 → 计划解释执行（agentic）→ SSE 推送。
 */
@Service
public class GenericPipelineService {

    private static final Logger log = LoggerFactory.getLogger(GenericPipelineService.class);
    private static final String NEED_CLARIFICATION = "NEED_CLARIFICATION";
    private static final String PROCEED = "PROCEED";

    private final ClarificationAnalyzer clarificationAnalyzer;
    private final QuestionReformulator questionReformulator;
    private final PlanPlanner planPlanner;
    private final PlanInterpreter planInterpreter;
    private final ObjectMapper objectMapper;

    public GenericPipelineService(
            ClarificationAnalyzer clarificationAnalyzer,
            QuestionReformulator questionReformulator,
            PlanPlanner planPlanner,
            PlanInterpreter planInterpreter,
            ObjectMapper objectMapper) {
        this.clarificationAnalyzer = clarificationAnalyzer;
        this.questionReformulator = questionReformulator;
        this.planPlanner = planPlanner;
        this.planInterpreter = planInterpreter;
        this.objectMapper = objectMapper;
    }

    /**
     * 统一入口：先分析是否缺条件；若需澄清则推送 clarification 并结束；
     * 否则补全问题 → 拆计划 → 用 agentic 执行 → 推送 plan / task 相关事件 / plan_done。
     */
    public void chat(String sessionId, String userInput, SseEmitter emitter) {
        try {
            // Step 1: 分析是否缺条件
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

            // Step 2: 补全/改写问题（尽量保留用户原文）
            String executableQuestion = questionReformulator.reformulate(sessionId, userInput);
            if (executableQuestion == null || executableQuestion.isBlank()) {
                executableQuestion = userInput;
            }
            sendEvent(emitter, "intent_clear", executableQuestion);

            // Step 3: 子问题拆分
            String planJson = planPlanner.plan(executableQuestion);
            PlanSchema plan = planInterpreter.parsePlan(planJson);
            if (log.isDebugEnabled()) {
                log.debug("子问题拆分 tasks={} execution={}", plan.tasks() != null ? plan.tasks().size() : 0, plan.execution());
            }
            if (plan.tasks().isEmpty()) {
                sendEvent(emitter, "plan_done", "{\"summary\":\"未拆分子任务\"}");
                emitter.complete();
                return;
            }
            sendEvent(emitter, "plan", objectMapper.writeValueAsString(plan));

            // Step 4 & 5: 计划解释 + agentic 执行（sequence / parallel_waves / supervisor）
            List<TaskSchema> sorted = planInterpreter.topologicalSort(plan);
            for (TaskSchema t : sorted) {
                sendEvent(emitter, "task_start", t.id());
            }
            Map<String, Object> results = planInterpreter.execute(plan, executableQuestion);

            if (results.containsKey("supervisorSummary")) {
                sendEvent(emitter, "plan_done", String.valueOf(results.get("supervisorSummary")));
            } else {
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
                }
                String summary = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
                sendEvent(emitter, "plan_done", summary);
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
