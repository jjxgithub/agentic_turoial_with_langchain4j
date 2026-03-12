package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

/**
 * Tool 注册时的元数据：重试次数、所属组、组内优先级。
 * 用于 {@link ToolRegistry} 的 register(toolId, tool, meta)；同组内按 priority 升序形成 fallback 链。
 */
public record ToolMeta(
        int retryCount,
        String groupId,
        int priority
) {
    public static final int DEFAULT_RETRY = 0;
    public static final int DEFAULT_PRIORITY = 0;

    public ToolMeta {
        if (retryCount < 0) retryCount = DEFAULT_RETRY;
    }

    public static ToolMeta of(int retryCount, String groupId, int priority) {
        return new ToolMeta(retryCount, groupId, priority);
    }

    public static ToolMeta retryOnly(int retryCount) {
        return new ToolMeta(retryCount, null, DEFAULT_PRIORITY);
    }

    public static ToolMeta group(String groupId, int priority) {
        return new ToolMeta(DEFAULT_RETRY, groupId, priority);
    }
}
