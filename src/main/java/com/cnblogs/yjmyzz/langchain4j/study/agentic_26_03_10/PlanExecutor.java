package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 按依赖关系分波次执行计划：同一波内并行，波与波之间顺序。
 */
@Component
public class PlanExecutor {

    private static final Logger log = LoggerFactory.getLogger(PlanExecutor.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()));

    private final SubQuestionAnswerer answerer;

    public PlanExecutor(SubQuestionAnswerer answerer) {
        this.answerer = answerer;
    }

    /**
     * 执行计划，返回每个任务 id 对应的结果。
     */
    public Map<String, String> execute(Plan plan) {
        Map<String, String> results = new ConcurrentHashMap<>();
        List<Task> remaining = new ArrayList<>(plan.tasks());

        while (!remaining.isEmpty()) {
            List<Task> ready = remaining.stream()
                    .filter(t -> results.keySet().containsAll(t.dependsOn()))
                    .toList();
            if (ready.isEmpty()) {
                log.warn("Plan has circular or missing dependency, remaining: {}", remaining);
                break;
            }
            remaining.removeAll(ready);

            List<CompletableFuture<Void>> futures = ready.stream()
                    .map(task -> CompletableFuture.runAsync(() -> {
                        String context = buildContext(task.dependsOn(), results);
                        String answer = answerer.answer(task.question(), context);
                        results.put(task.id(), answer != null ? answer : "");
                    }, executor))
                    .toList();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        }
        return results;
    }

    private static String buildContext(List<String> dependsOn, Map<String, String> results) {
        if (dependsOn == null || dependsOn.isEmpty()) {
            return "（无）";
        }
        StringBuilder sb = new StringBuilder();
        for (String id : dependsOn) {
            String r = results.get(id);
            if (r != null) {
                sb.append("[").append(id).append("] ").append(r).append("\n");
            }
        }
        return sb.length() > 0 ? sb.toString() : "（无）";
    }
}
