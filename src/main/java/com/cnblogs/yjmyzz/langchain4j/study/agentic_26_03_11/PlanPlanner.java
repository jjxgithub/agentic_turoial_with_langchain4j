package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 通用子问题拆分：将一条可执行问题拆成多个子任务，依赖、可选条件、执行模式。
 * 输出仅为一合法 JSON，可解析为 {@link PlanSchema}。
 */
public interface PlanPlanner {

    @SystemMessage("""
        将用户的「可执行问题」拆分为多个子问题（子任务），并标明执行依赖与可选条件、执行模式。
        输出必须是且仅是一段合法 JSON，不要包含任何其他文字、markdown 标记或代码块包裹。
        JSON 格式如下（所有字段名严格一致）：
        {
          "execution": "parallel_waves",
          "tasks": [
            {"id":"A","question":"子问题描述","dependsOn":[]},
            {"id":"B","question":"...","dependsOn":["A"],"condition":{"sourceKey":"A","op":"contains","value":"雨"}},
            {"id":"C","question":"...","dependsOn":["A","B"]}
          ]
        }
        规则：
        - execution：必填。取值为 "sequence"（严格顺序）、"parallel_waves"（同层并行）、"supervisor"（监督者动态选子任务）。默认 "parallel_waves"。
        - id：唯一标识，建议 A、B、C 等。
        - question：该子问题的完整描述。
        - dependsOn：该任务依赖的任务 id 数组。空数组表示可立即执行；非空表示需等所列任务全部完成后再执行。
        - condition：可选。仅当需要「根据前置结果决定是否执行」时填写。sourceKey 为依赖的 taskId（如 "A"），op 取 contains/equals/notContains/present/absent，value 为比较值（present/absent 可省略 value）。
        - 能并发的标为无依赖或依赖已满足；有先后顺序的用 dependsOn 表达。
        - 若问题本身只对应一个简单任务，也输出仅含一个任务的数组，execution 仍必填。
        - 不绑定任何具体业务领域，通用拆分即可。
        """)
    @UserMessage("可执行问题：{{executableQuestion}}")
    String plan(String executableQuestion);
}
