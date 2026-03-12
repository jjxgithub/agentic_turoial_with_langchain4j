package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.Skill;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillRegistry;

/**
 * 在「问题补全 → 拆分 → 编排」之后，通过 skill 列表发现应由哪个 Agent 处理；
 * 若命中 skill，则交给该 skill 的 Agent（内部可多步）执行，否则走通用 plan 执行。
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
    private final ObjectMapper objectMapper;

    public SkillAwarePipelineService(
            ClarificationAnalyzer clarificationAnalyzer,
            QuestionReformulator questionReformulator,
            PlanPlanner planPlanner,
            PlanInterpreter planInterpreter,
            SkillRegistry skillRegistry,
            ObjectMapper objectMapper) {
        this.clarificationAnalyzer = clarificationAnalyzer;
        this.questionReformulator = questionReformulator;
        this.planPlanner = planPlanner;
        this.planInterpreter = planInterpreter;
        this.skillRegistry = skillRegistry;
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

            Optional<Skill> matched = skillRegistry.findMatch(executableQuestion);
            if (matched.isPresent()) {
                Skill skill = matched.get();
                sendEvent(emitter, "skill_matched", skill.id());
                String result = skill.handler().handle(executableQuestion, plan);
                sendEvent(emitter, "plan_done", result != null ? result : "{\"summary\":\"skill 执行完成\"}");
            } else {
                List<TaskSchema> sorted = planInterpreter.topologicalSort(plan);
                for (TaskSchema t : sorted) sendEvent(emitter, "task_start", t.id());
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
                    sendEvent(emitter, "plan_done", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results));
                }
            }
            emitter.complete();
        } catch (Exception e) {
            log.warn("skill-aware chat error, sessionId={}", sessionId, e);
            try { sendEvent(emitter, "error", e.getMessage()); } catch (IOException ignored) {}
            emitter.completeWithError(e);
        }
    }

    private static void sendEvent(SseEmitter emitter, String eventType, String data) throws IOException {
        if (data != null) emitter.send(SseEmitter.event().name(eventType).data(data));
        else emitter.send(SseEmitter.event().name(eventType));
    }
}
