package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tool 注册表：按 id 注册/解析 Tool 实例，支持单 tool、分组（groupId）+ 优先级与重试。
 * step 的 toolIds 可为单个 tool id 或 groupId；groupId 时返回一个 {@link GroupTool}（组内按 priority 升序，失败/无结果时 fallback）。
 * <p>
 * 示例：注册「搜索」组，百度优先、谷歌备用，每组员重试 1 次：
 * <pre>
 * registry.registerGroup("search", "搜索", "执行搜索，内部按顺序尝试多个引擎")
 *   .register("baidu", baiduTool, ToolMeta.of(1, "search", 1))
 *   .register("google", googleTool, ToolMeta.of(1, "search", 2));
 * </pre>
 * step 的 toolIds 填 ["search"] 即对该步暴露一个 GroupTool。
 */
public class ToolRegistry {

    private final Map<String, Object> byId = new ConcurrentHashMap<>();
    private final Map<String, ToolMeta> metaById = new ConcurrentHashMap<>();
    private final Map<String, GroupDef> groups = new ConcurrentHashMap<>();

    /** 注册单个 tool，无重试、无分组。 */
    public ToolRegistry register(String toolId, Object tool) {
        return register(toolId, tool, null);
    }

    /** 注册单个 tool，并指定元数据（重试、分组、优先级）。 */
    public ToolRegistry register(String toolId, Object tool, ToolMeta meta) {
        if (toolId == null || toolId.isBlank() || tool == null) return this;
        String id = toolId.trim();
        byId.put(id, tool);
        metaById.put(id, meta != null ? meta : new ToolMeta(ToolMeta.DEFAULT_RETRY, null, ToolMeta.DEFAULT_PRIORITY));
        return this;
    }

    /** 注册一个组定义（displayName/description 用于日志与扩展）；成员通过 register(toolId, tool, ToolMeta.group(groupId, priority)) 加入。 */
    public ToolRegistry registerGroup(String groupId, String displayName, String description) {
        if (groupId != null && !groupId.isBlank()) {
            groups.put(groupId.trim(), new GroupDef(groupId.trim(), displayName, description));
        }
        return this;
    }

    /**
     * 按 id 列表解析出一组 Tool 实例（id 可为 toolId 或 groupId）。
     * 若 id 为已注册的 groupId，则返回一个 GroupTool（组内按 priority 排序，含重试与 fallback）；否则返回该 tool（未包装重试）。
     *
     * @param toolIds 配置中的 tool id 或 group id 列表，可为 null 或空
     * @return 可传给 AiServices.builder(...).tools(...) 的实例列表
     */
    public List<Object> getTools(List<String> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) return List.of();
        List<Object> out = new ArrayList<>();
        for (String id : toolIds) {
            if (id == null || id.isBlank()) continue;
            String key = id.trim();
            if (groups.containsKey(key)) {
                Object groupTool = buildGroupTool(key);
                if (groupTool != null) out.add(groupTool);
            } else {
                Object t = byId.get(key);
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    private Object buildGroupTool(String groupId) {
        GroupDef def = groups.get(groupId);
        if (def == null) return null;
        List<GroupTool.Member> members = metaById.entrySet().stream()
                .filter(e -> groupId.equals(e.getValue().groupId()))
                .filter(e -> byId.get(e.getKey()) != null)
                .sorted(Comparator.comparingInt(e -> e.getValue().priority()))
                .map(e -> new GroupTool.Member(byId.get(e.getKey()), e.getValue().retryCount()))
                .collect(Collectors.toList());
        if (members.isEmpty()) return null;
        return new GroupTool(def.groupId(), def.displayName(), def.description(), members, this::isEmptyResult);
    }

    private boolean isEmptyResult(String s) {
        return s == null || s.isBlank();
    }
}
