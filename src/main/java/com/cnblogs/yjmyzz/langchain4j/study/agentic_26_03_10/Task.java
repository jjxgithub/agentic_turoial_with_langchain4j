package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 子问题/子任务：id、问题描述、依赖的任务 id 列表。
 * dependsOn 为空表示可立即执行；非空表示需等依赖任务完成后执行。
 */
public record Task(
        String id,
        String question,
        List<String> dependsOn
) {
    @JsonCreator
    public Task(
            @JsonProperty("id") String id,
            @JsonProperty("question") String question,
            @JsonProperty("dependsOn") List<String> dependsOn) {
        this.id = id == null ? "" : id;
        this.question = question == null ? "" : question;
        this.dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    }
}
