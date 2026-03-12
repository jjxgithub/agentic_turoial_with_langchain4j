package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 注册表：按 id 注册/解析 Tool 实例，供 StepDef.toolIds 在构建 Agent 时解析为可传入 AiServices 的 tool 对象。
 * 与具体业务解耦，扩展时只需 register(id, toolInstance)。
 */
public class ToolRegistry {

    private final Map<String, Object> byId = new ConcurrentHashMap<>();

    public ToolRegistry register(String toolId, Object tool) {
        if (toolId != null && !toolId.isBlank() && tool != null) {
            byId.put(toolId.trim(), tool);
        }
        return this;
    }

    /**
     * 按 id 列表解析出一组 Tool 实例（未注册的 id 会跳过，不抛异常）。
     *
     * @param toolIds 配置中的 tool id 列表，可为 null 或空
     * @return 已注册的 tool 实例列表，可直接传给 AiServices.builder(...).tools(...)
     */
    public List<Object> getTools(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return List.of();
        }
        List<Object> out = new ArrayList<>();
        for (String id : toolIds) {
            if (id == null || id.isBlank()) continue;
            Object t = byId.get(id.trim());
            if (t != null) out.add(t);
        }
        return out;
    }
}
