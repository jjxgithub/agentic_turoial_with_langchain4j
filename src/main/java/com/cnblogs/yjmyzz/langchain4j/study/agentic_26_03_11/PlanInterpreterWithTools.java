package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools.EmailNotifyTool311;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools.WeatherQueryTool311;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 计划解释器（单 Agent + 全量工具版）：与 PlanInterpreter 相同流程，但任务由 UnifiedTaskAgentWithTools 执行（绑定工具）。
 */
@Service
public class PlanInterpreterWithTools {

    private static final Logger log = LoggerFactory.getLogger(PlanInterpreterWithTools.class);
    private static final Pattern JSON_BLOCK = Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");
    private static final String JUDGE_SUFFIX = "_judge";

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final UnifiedTaskAgentWithTools directAgentWithTools;
    private final WeatherQueryTool311 weatherQueryTool311;
    private final EmailNotifyTool311 emailNotifyTool311;

    public PlanInterpreterWithTools(ChatModel chatModel, ObjectMapper objectMapper,
                                     @Qualifier("unifiedTaskAgentWithToolsForDirectCall") UnifiedTaskAgentWithTools directAgentWithTools,
                                     WeatherQueryTool311 weatherQueryTool311, EmailNotifyTool311 emailNotifyTool311) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.directAgentWithTools = directAgentWithTools;
        this.weatherQueryTool311 = weatherQueryTool311;
        this.emailNotifyTool311 = emailNotifyTool311;
    }

    public PlanSchema parsePlan(String raw) {
        String json = raw;
        var matcher = JSON_BLOCK.matcher(raw);
        if (matcher.find()) json = matcher.group(1).trim();
        try {
            return objectMapper.readValue(json, PlanSchema.class);
        } catch (Exception e) {
            String rawSnippet = raw != null && raw.length() > 200 ? raw.substring(0, 200) + "..." : raw;
            log.warn("parsePlan failed, rawLen={} rawSnippet={}", raw != null ? raw.length() : 0, rawSnippet, e);
            return new PlanSchema(List.of(), PlanSchema.EXECUTION_PARALLEL_WAVES);
        }
    }

    public List<List<TaskSchema>> waves(PlanSchema plan) {
        if (plan == null || plan.tasks().isEmpty()) return List.of();
        Set<String> done = new HashSet<>();
        List<List<TaskSchema>> wavesOut = new ArrayList<>();
        List<TaskSchema> remaining = new ArrayList<>(plan.tasks());
        int maxIter = plan.tasks().size() * 2;
        for (int i = 0; i < maxIter && !remaining.isEmpty(); i++) {
            List<TaskSchema> ready = remaining.stream()
                    .filter(t -> t.dependsOn().stream().allMatch(done::contains))
                    .toList();
            if (ready.isEmpty()) break;
            remaining.removeAll(ready);
            ready.forEach(t -> done.add(t.id()));
            wavesOut.add(new ArrayList<>(ready));
        }
        return wavesOut;
    }

    public List<TaskSchema> topologicalSort(PlanSchema plan) {
        return waves(plan).stream().flatMap(List::stream).collect(Collectors.toList());
    }

    public static boolean evaluateCondition(ConditionSchema c, AgenticScope scope) {
        if (c == null || c.sourceKey().isBlank()) return true;
        String raw = String.valueOf(scope.readState(c.sourceKey(), ""));
        String op = c.op() == null ? "" : c.op().toLowerCase();
        String val = c.value() == null ? "" : c.value();
        switch (op) {
            case "contains":
                return raw.contains(val);
            case "equals":
                return raw.equals(val);
            case "notcontains":
                return !raw.contains(val);
            case "present":
                return raw != null && !raw.isBlank();
            case "absent":
                return raw == null || raw.isBlank();
            default:
                return raw.contains(val);
        }
    }

    private static boolean dependencyJudgesAllSuccess(TaskSchema t, AgenticScope scope) {
        if (t.dependsOn() == null || t.dependsOn().isEmpty()) return true;
        for (String depId : t.dependsOn()) {
            String verdict = String.valueOf(scope.readState(depId + JUDGE_SUFFIX, "")).trim();
            if (!DependencySuccessJudge.SUCCESS.equalsIgnoreCase(verdict)) return false;
        }
        return true;
    }

    public static boolean evaluateConditions(TaskSchema t, AgenticScope scope) {
        if (!dependencyJudgesAllSuccess(t, scope)) return false;
        if (t.conditions() != null && !t.conditions().isEmpty()) {
            return t.conditions().stream().allMatch(c -> evaluateCondition(c, scope));
        }
        if (t.condition() != null && !t.condition().sourceKey().isBlank()) {
            return evaluateCondition(t.condition(), scope);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(PlanSchema plan, String executableQuestion) {
        String exec = plan.execution() == null ? PlanSchema.EXECUTION_PARALLEL_WAVES : plan.execution();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("reformulatedQuestion", executableQuestion != null ? executableQuestion : "");

        if (PlanSchema.EXECUTION_SUPERVISOR.equals(exec)) {
            SupervisorAgent supervisor = buildSupervisorAgent(plan.tasks());
            String request = (String) input.get("reformulatedQuestion");
            Object summary = supervisor.invoke(request != null ? request : "");
            return Map.of("supervisorSummary", summary != null ? summary : "");
        }
        UntypedAgent workflow = PlanSchema.EXECUTION_SEQUENCE.equals(exec)
                ? buildSequence(plan.tasks())
                : buildParallelWaves(plan);
        Object result = workflow.invoke(input);
        if (result instanceof Map) return (Map<String, Object>) result;
        return Map.of();
    }

    UntypedAgent buildSequence(List<TaskSchema> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return AgenticServices.sequenceBuilder().output(scope -> Map.of()).build();
        }
        List<Object> subAgents = new ArrayList<>();
        for (TaskSchema t : tasks) {
            subAgents.add(scopeSetter(t));
            Object agent = taskAgentWithTools(t.id());
            if (t.hasCondition()) {
                agent = AgenticServices.conditionalBuilder()
                        .subAgents(scope -> evaluateConditions(t, scope), agent)
                        .outputKey(t.id())
                        .build();
            }
            subAgents.add(agent);
            subAgents.add(scopeSetterForJudge(t));
            subAgents.add(judgeAgent(t.id()));
        }
        return AgenticServices.sequenceBuilder()
                .subAgents(subAgents.toArray())
                .output(scope -> collectResults(scope, tasks))
                .build();
    }

    UntypedAgent buildParallelWaves(PlanSchema plan) {
        List<List<TaskSchema>> waveList = waves(plan);
        if (waveList.isEmpty()) return AgenticServices.sequenceBuilder().output(scope -> Map.of()).build();

        List<Object> steps = new ArrayList<>();
        for (List<TaskSchema> wave : waveList) {
            if (wave.size() == 1) {
                TaskSchema t = wave.get(0);
                steps.add(scopeSetter(t));
                Object agent = taskAgentWithTools(t.id());
                if (t.hasCondition()) {
                    agent = AgenticServices.conditionalBuilder()
                            .subAgents(scope -> evaluateConditions(t, scope), agent)
                            .outputKey(t.id())
                            .build();
                }
                steps.add(agent);
                steps.add(scopeSetterForJudge(t));
                steps.add(judgeAgent(t.id()));
            } else {
                steps.add(AgenticServices.agentAction(scope -> {
                    for (TaskSchema t : wave) {
                        String ctx = buildContextFromScope(t.dependsOn(), scope);
                        scope.writeState("question_" + t.id(), t.question());
                        scope.writeState("context_" + t.id(), ctx);
                    }
                }));
                steps.add(AgenticServices.agentAction(scope -> {
                    for (TaskSchema t : wave) {
                        if (t.hasCondition() && !evaluateConditions(t, scope)) continue;
                        String q = String.valueOf(scope.readState("question_" + t.id(), ""));
                        String ctx = String.valueOf(scope.readState("context_" + t.id(), ""));
                        String res = directAgentWithTools.execute(q, ctx);
                        scope.writeState(t.id(), res);
                    }
                }));
                for (TaskSchema t : wave) {
                    steps.add(scopeSetterForJudge(t));
                    steps.add(judgeAgent(t.id()));
                }
            }
        }
        List<TaskSchema> allTasks = waveList.stream().flatMap(List::stream).collect(Collectors.toList());
        return AgenticServices.sequenceBuilder()
                .subAgents(steps.toArray())
                .output(scope -> collectResults(scope, allTasks))
                .build();
    }

    SupervisorAgent buildSupervisorAgent(List<TaskSchema> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("supervisor 模式需要至少一个任务");
        }
        List<Object> subAgents = new ArrayList<>();
        for (TaskSchema t : tasks) {
            Object seq = AgenticServices.sequenceBuilder()
                    .subAgents(scopeSetter(t), taskAgentWithTools(t.id()))
                    .outputKey(t.id())
                    .build();
            subAgents.add(seq);
        }
        return AgenticServices.supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(subAgents.toArray())
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY_AND_SUMMARIZATION)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .supervisorContext("按需调用子任务完成用户请求。每次只调用一个子任务。用中文回答。")
                .build();
    }

    private Object scopeSetter(TaskSchema t) {
        return AgenticServices.agentAction(scope -> {
            String ctx = buildContextFromScope(t.dependsOn(), scope);
            scope.writeState("currentQuestion", t.question());
            scope.writeState("contextFromDependencies", ctx);
        });
    }

    private Object taskAgentWithTools(String outputKey) {
        return AgenticServices.agentBuilder(UnifiedTaskAgentWithTools.class)
                .chatModel(chatModel)
                .tools(weatherQueryTool311, emailNotifyTool311)
                .outputKey(outputKey)
                .build();
    }

    private Object scopeSetterForJudge(TaskSchema t) {
        return AgenticServices.agentAction(scope -> {
            String result = String.valueOf(scope.readState(t.id(), ""));
            scope.writeState("currentJudgeTaskId", t.id());
            scope.writeState("currentJudgeTaskQuestion", t.question());
            scope.writeState("currentJudgeTaskResult", result);
        });
    }

    private Object judgeAgent(String taskId) {
        return AgenticServices.agentBuilder(DependencySuccessJudge.class)
                .chatModel(chatModel)
                .outputKey(taskId + JUDGE_SUFFIX)
                .build();
    }

    private static String buildContextFromScope(List<String> dependsOn, AgenticScope scope) {
        if (dependsOn == null || dependsOn.isEmpty()) return "（无）";
        StringBuilder sb = new StringBuilder();
        for (String id : dependsOn) {
            Object v = scope.readState(id, "");
            sb.append("[").append(id).append("] ").append(v).append("\n");
        }
        return sb.length() > 0 ? sb.toString() : "（无）";
    }

    private static Map<String, Object> collectResults(AgenticScope scope, List<TaskSchema> tasks) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (TaskSchema t : tasks) {
            out.put(t.id(), scope.readState(t.id(), ""));
        }
        return out;
    }
}
