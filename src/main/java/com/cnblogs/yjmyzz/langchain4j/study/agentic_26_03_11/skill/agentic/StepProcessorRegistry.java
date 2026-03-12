package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.util.HashMap;
import java.util.Map;

/**
 * processorId → StepProcessor 的注册表。各 skill 可注册自己的前/后处理（查库、调接口等），StepDef 通过 preProcessorId/postProcessorId 引用。
 */
public class StepProcessorRegistry {

    private final Map<String, StepProcessor> processors = new HashMap<>();

    public StepProcessorRegistry register(String processorId, StepProcessor processor) {
        if (processorId != null && !processorId.isBlank() && processor != null) {
            processors.put(processorId, processor);
        }
        return this;
    }

    public StepProcessor get(String processorId) {
        return processorId == null ? null : processors.get(processorId);
    }

    public boolean has(String processorId) {
        return processorId != null && processors.containsKey(processorId);
    }
}
