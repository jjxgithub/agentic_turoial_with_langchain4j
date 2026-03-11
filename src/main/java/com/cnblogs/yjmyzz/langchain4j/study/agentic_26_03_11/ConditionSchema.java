package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 条件表达式：在 scope 上求值，用于 conditional 分支。
 * 格式：从 scope 读取 sourceKey 对应的值，用 op 与 value 比较。
 * 支持：contains, equals, notContains, present, absent。
 */
public record ConditionSchema(
        String sourceKey,
        String op,
        String value
) {
    /** contains: 字符串包含 value；equals: 字符串相等；notContains: 不包含；present: 存在且非空；absent: 不存在或空 */
    @JsonCreator
    public ConditionSchema(
            @JsonProperty("sourceKey") String sourceKey,
            @JsonProperty("op") String op,
            @JsonProperty("value") String value) {
        this.sourceKey = sourceKey == null ? "" : sourceKey;
        this.op = op == null ? "" : op;
        this.value = value == null ? "" : value;
    }
}
