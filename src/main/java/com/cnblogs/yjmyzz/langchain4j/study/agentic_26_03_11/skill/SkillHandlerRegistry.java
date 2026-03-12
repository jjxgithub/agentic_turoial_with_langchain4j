package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * handlerId → SkillHandler 的注册表，供从 SKILL.md 加载时解析 handlerId 得到执行体。
 */
public class SkillHandlerRegistry {

    private final Map<String, SkillHandler> handlers = new ConcurrentHashMap<>();

    public SkillHandlerRegistry register(String handlerId, SkillHandler handler) {
        if (handlerId != null && !handlerId.isBlank() && handler != null) {
            handlers.put(handlerId.strip(), handler);
        }
        return this;
    }

    public SkillHandler get(String handlerId) {
        return handlerId == null ? null : handlers.get(handlerId.strip());
    }
}
