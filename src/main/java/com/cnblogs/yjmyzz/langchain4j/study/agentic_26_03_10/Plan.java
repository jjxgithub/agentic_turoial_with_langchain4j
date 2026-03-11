package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 编排计划：由用户输入拆分出的子任务列表。
 * 通过 dependsOn 表达并行（无依赖可同时执行）与顺序（有依赖则等前置完成）。
 */
public record Plan(List<Task> tasks) {
    @JsonCreator
    public Plan(@JsonProperty("tasks") List<Task> tasks) {
        this.tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }
}
