package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.Skill;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillRegistry;

/**
 * 在「问题补全 → 拆分 → 编排」之后，按子问题（每个 task）做 skill 匹配：
 * 每个子任务用其 question 匹配一次，命中则交该 skill 的 Agent 执行，否则走通用 Agent。
 */
@Service
public class SkillAwarePipelineService {

    private static final Logger log = LoggerFactory.getLogger(SkillAwarePipelineService.class);
    private static final String NEED_CLARIFICATION = "NEED_CLARIFICATION";

    private final ClarificationAnalyzer clarificationAnalyzer;
    private final QuestionReformulator questionReformulator;
    private final PlanPlanner planPlanner;
    private final PlanInterpreter planInterpreter;
    private final SkillRegistry skillRegistry;
    private final GenericTaskAgent directTaskAgent;
    private final ObjectMapper objectMapper;

    public SkillAwarePipelineService(
            ClarificationAnalyzer clarificationAnalyzer,
            QuestionReformulator questionReformulator,
            PlanPlanner planPlanner,
            PlanInterpreter planInterpreter,
            SkillRegistry skillRegistry,
            @Qualifier("genericTaskAgentForDirectCall") GenericTaskAgent directTaskAgent,
            ObjectMapper objectMapper) {
        this.clarificationAnalyzer = clarificationAnalyzer;
        this.questionReformulator = questionReformulator;
        this.planPlanner = planPlanner;
        this.planInterpreter = planInterpreter;
        this.skillRegistry = skillRegistry;
        this.directTaskAgent = directTaskAgent;
        this.objectMapper = objectMapper;
    }

    public void chat(String sessionId, String userInput, SseEmitter emitter) {
        try {
//            String analysis = clarificationAnalyzer.analyze(sessionId, userInput);
//            if (analysis == null || analysis.isBlank()) {
//                sendEvent(emitter, "clarification", "请再具体说明一下您的需求。");
//                sendEvent(emitter, "done", null);
//                emitter.complete();
//                return;
//            }

//            String firstLine = analysis.lines().findFirst().orElse("").strip();
//            String content = analysis.contains("\n")
//                    ? analysis.substring(analysis.indexOf('\n') + 1).trim()
//                    : "";
//            if (NEED_CLARIFICATION.equalsIgnoreCase(firstLine)) {
//                sendEvent(emitter, "clarification", content);
//                sendEvent(emitter, "done", null);
//                emitter.complete();
//                return;
//            }

            String executableQuestion = questionReformulator.reformulate(sessionId, userInput);
            if (executableQuestion == null || executableQuestion.isBlank()) executableQuestion = userInput;
            sendEvent(emitter, "intent_clear", executableQuestion);

            String planJson = planPlanner.plan(executableQuestion);
            PlanSchema plan = planInterpreter.parsePlan(planJson);
            if (plan.tasks().isEmpty()) {
                sendEvent(emitter, "plan_done", "{\"summary\":\"未拆分子任务\"}");
                emitter.complete();
                return;
            }
            sendEvent(emitter, "plan", objectMapper.writeValueAsString(plan));

            List<TaskSchema> sorted = planInterpreter.topologicalSort(plan);
            Map<String, Object> results = new LinkedHashMap<>();
            for (TaskSchema t : sorted) {
                sendEvent(emitter, "task_start", t.id());
                String taskQuestion = t.question() != null ? t.question() : "";
                Optional<Skill> matched = skillRegistry.findMatch(taskQuestion);
                String resultStr;
                if (matched.isPresent()) {
                    Skill skill = matched.get();
                    sendEvent(emitter, "skill_matched", skill.id());
                    PlanSchema singleTaskPlan = new PlanSchema(List.of(t), PlanSchema.EXECUTION_SEQUENCE);
                    String result = skill.handler().handle(taskQuestion, singleTaskPlan);
                    resultStr = result != null ? result : "";
                } else {
                    String context = buildContextFromDependencies(t.dependsOn(), results);
                    resultStr = directTaskAgent.execute(taskQuestion, context);
                    if (resultStr == null) resultStr = "";
                }
                results.put(t.id(), resultStr);
                String taskResultJson = objectMapper.createObjectNode()
                        .put("taskId", t.id())
                        .put("question", taskQuestion)
                        .put("result", resultStr)
                        .toString();
                sendEvent(emitter, "task_result", taskResultJson);
                sendEvent(emitter, "task_end", t.id());
            }
            sendEvent(emitter, "plan_done", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
            emitter.complete();
        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("skill-aware chat error sessionId={} error={}", sessionId, errMsg, e);
            try { sendEvent(emitter, "error", e.getMessage()); } catch (IOException ignored) {}
            emitter.completeWithError(e);
        }
    }

    private static String buildContextFromDependencies(List<String> dependsOn, Map<String, Object> results) {
        if (dependsOn == null || dependsOn.isEmpty()) return "（无）";
        StringBuilder sb = new StringBuilder();
        for (String id : dependsOn) {
            Object v = results.get(id);
            sb.append("[").append(id).append("] ").append(v != null ? v : "").append("\n");
        }
        return sb.length() > 0 ? sb.toString() : "（无）";
    }

    private static void sendEvent(SseEmitter emitter, String eventType, String data) throws IOException {
        if (data != null) emitter.send(SseEmitter.event().name(eventType).data(data));
        else emitter.send(SseEmitter.event().name(eventType));
    }
}
