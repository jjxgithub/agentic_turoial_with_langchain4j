package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 通用子任务 schema：id、问题描述、依赖、可选条件（可多个，全部满足才执行）。
 * dependsOn 为空表示可立即执行；非空表示需等依赖任务完成后执行。
 * condition/conditions 非空时仅当条件在 scope 上全部成立时才执行该任务（conditional）。
 */
public record TaskSchema(
        String id,
        String question,
        List<String> dependsOn,
        ConditionSchema condition,
        List<ConditionSchema> conditions
) {
    @JsonCreator
    public TaskSchema(
            @JsonProperty("id") String id,
            @JsonProperty("question") String question,
            @JsonProperty("dependsOn") List<String> dependsOn,
            @JsonProperty("condition") ConditionSchema condition,
            @JsonProperty("conditions") List<ConditionSchema> conditions) {
        this.id = id == null ? "" : id;
        this.question = question == null ? "" : question;
        this.dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        this.condition = condition;
        this.conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    /** 用于解释器：若配置了 conditions 则用列表（全部满足）；否则用单个 condition。 */
    public boolean hasCondition() {
        return (conditions != null && !conditions.isEmpty()) || (condition != null && !condition.sourceKey().isBlank());
    }
}
