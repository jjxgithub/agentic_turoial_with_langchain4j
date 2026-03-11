package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 将用户输入拆分为多个子问题，并标明依赖关系。
 * 输出仅为一合法 JSON，便于解析为 {@link Plan}。
 */
public interface PlanPlanner {

    @SystemMessage("""
        你将用户的输入拆分为多个子问题（子任务），并标明执行依赖。
        输出必须是且仅是一段合法 JSON，不要包含任何其他文字、markdown 标记或代码块包裹。
        JSON 格式严格如下：
        {"tasks":[{"id":"A","question":"子问题描述","dependsOn":[]},{"id":"B","question":"...","dependsOn":[]},{"id":"C","question":"...","dependsOn":["A","B"]}]}
        规则：
        - id：唯一标识，建议 A、B、C 等。
        - question：该子问题的完整描述。
        - dependsOn：该任务依赖的任务 id 数组。空数组 [] 表示可立即执行（可与同批其他无依赖任务并发）；非空表示需等所列任务全部完成后再执行（如 C 依赖 A 和 B 则 dependsOn 为 ["A","B"]）。
        - 能并发的就标为无依赖或依赖已满足；有先后顺序的用 dependsOn 表达。
        - 若用户输入本身只对应一个简单问题，也输出一个任务的数组。
        """)
    @UserMessage("用户输入：{{userInput}}")
    String plan(String userInput);
}
