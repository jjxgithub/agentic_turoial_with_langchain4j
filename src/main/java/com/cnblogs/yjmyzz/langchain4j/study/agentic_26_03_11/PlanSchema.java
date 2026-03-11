package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 通用计划 schema：子任务列表 + 执行模式。
 * execution: sequence（严格顺序）、parallel_waves（同层并行）、supervisor（监督者动态选子任务）。
 */
public record PlanSchema(
        List<TaskSchema> tasks,
        String execution
) {
    public static final String EXECUTION_SEQUENCE = "sequence";
    public static final String EXECUTION_PARALLEL_WAVES = "parallel_waves";
    public static final String EXECUTION_SUPERVISOR = "supervisor";

    @JsonCreator
    public PlanSchema(
            @JsonProperty("tasks") List<TaskSchema> tasks,
            @JsonProperty("execution") String execution) {
        this.tasks = tasks == null ? List.of() : List.copyOf(tasks);
        this.execution = execution == null || execution.isBlank() ? EXECUTION_PARALLEL_WAVES : execution;
    }
}
