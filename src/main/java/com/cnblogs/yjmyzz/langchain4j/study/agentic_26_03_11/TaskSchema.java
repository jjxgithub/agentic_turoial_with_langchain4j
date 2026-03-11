package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 通用子任务 schema：id、问题描述、依赖、可选条件。
 * dependsOn 为空表示可立即执行；非空表示需等依赖任务完成后执行。
 * condition 非空时仅当条件在 scope 上成立时才执行该任务（conditional）。
 */
public record TaskSchema(
        String id,
        String question,
        List<String> dependsOn,
        ConditionSchema condition
) {
    @JsonCreator
    public TaskSchema(
            @JsonProperty("id") String id,
            @JsonProperty("question") String question,
            @JsonProperty("dependsOn") List<String> dependsOn,
            @JsonProperty("condition") ConditionSchema condition) {
        this.id = id == null ? "" : id;
        this.question = question == null ? "" : question;
        this.dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        this.condition = condition;
    }
}
