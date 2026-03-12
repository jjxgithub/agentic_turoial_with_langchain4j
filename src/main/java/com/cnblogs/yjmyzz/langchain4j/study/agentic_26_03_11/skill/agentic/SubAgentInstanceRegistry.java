package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.util.HashMap;
import java.util.Map;

/**
 * agentId → SubAgent 实例的注册表。当某步开启 catchAgentError 时，Runner 通过本注册表取实例在 agentAction 内调用并 try-catch，以支持「本步异常不中断整链」。
 * 可选：不注册则 catchAgentError 时仍走 agentBuilder（异常会中断整链）。
 */
public class SubAgentInstanceRegistry {

    private final Map<String, SubAgent> instanceById = new HashMap<>();

    public SubAgentInstanceRegistry register(String agentId, SubAgent instance) {
        if (agentId != null && !agentId.isBlank() && instance != null) {
            instanceById.put(agentId, instance);
        }
        return this;
    }

    public SubAgent get(String agentId) {
        return agentId == null ? null : instanceById.get(agentId);
    }
}
