package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

/**
 * 对 LLM 暴露为单一逻辑 Tool；内部按成员顺序执行，每个成员可配置重试，失败或「无结果」时 fallback 到下一成员。
 * 由 {@link ToolRegistry} 在 step 的 toolIds 中出现 groupId 时构建并返回。
 */
public final class GroupTool {

    private static final Logger log = LoggerFactory.getLogger(GroupTool.class);

    private final String groupId;
    private final String displayName;
    private final String description;
    private final List<Member> members;
    private final Predicate<String> emptyResult;

    public GroupTool(String groupId, String displayName, String description, List<Member> members, Predicate<String> emptyResult) {
        this.groupId = groupId != null ? groupId : "";
        this.displayName = displayName != null ? displayName : this.groupId;
        this.description = description != null ? description : "";
        this.members = members != null ? List.copyOf(members) : List.of();
        this.emptyResult = emptyResult != null ? emptyResult : (s -> s == null || s.isBlank());
    }

    @Tool("按优先级依次尝试组内工具，失败或无结果时自动切换下一项。输入将传递给组内工具链。")
    public String execute(@P("输入参数，将传递给组内工具链") String input) {
        for (int i = 0; i < members.size(); i++) {
            Member member = members.get(i);
            int maxAttempts = 1 + Math.max(0, member.retryCount());
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    String result = ToolInvoker.invoke(member.tool(), input);
                    if (result != null && !emptyResult.test(result)) {
                        return result;
                    }
                    if (attempt < maxAttempts) {
                        log.debug("GroupTool {} member[{}] attempt {}/{} returned empty, retrying", groupId, i, attempt, maxAttempts);
                    }
                } catch (Exception e) {
                    if (attempt < maxAttempts) {
                        log.warn("GroupTool {} member[{}] attempt {}/{} failed, retrying", groupId, i, attempt, maxAttempts, e);
                    } else {
                        log.warn("GroupTool {} member[{}] failed after {} attempt(s), fallback to next", groupId, i, maxAttempts, e);
                    }
                }
            }
        }
        return "";
    }

    /** 组内成员：tool 实例 + 该成员的重试次数。 */
    public record Member(Object tool, int retryCount) {}
}
