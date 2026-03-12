package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.util.HashMap;
import java.util.Map;

/**
 * agentId → SubAgent 实现类的注册表，供 SkillWorkflowRunner 按步骤查找并用不同 outputKey 构建每步 Agent。
 * 平台扩展：新步骤 = 新 Agent 接口类 + 本注册表 register(agentId, agentClass)。
 */
@SuppressWarnings("unchecked")
public class SubAgentRegistry {

    private final Map<String, Class<?>> agentClassById = new HashMap<>();

    public SubAgentRegistry register(String agentId, Class<? extends SubAgent> agentClass) {
        if (agentId != null && !agentId.isBlank() && agentClass != null) {
            agentClassById.put(agentId, agentClass);
        }
        return this;
    }

    public Class<? extends SubAgent> getAgentClass(String agentId) {
        return (Class<? extends SubAgent>) agentClassById.get(agentId);
    }

    public boolean has(String agentId) {
        return agentId != null && agentClassById.containsKey(agentId);
    }
}
