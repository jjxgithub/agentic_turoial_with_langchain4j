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
          "execution": "sequence",
          "tasks": [
            {"id":"A","question":"子问题描述","dependsOn":[]},
            {"id":"B","question":"...","dependsOn":["A"],"condition":{"sourceKey":"A","op":"notContains","value":"某关键字"}},
            {"id":"C","question":"...","dependsOn":["A","B"],"conditions":[{"sourceKey":"A","op":"present","value":""},{"sourceKey":"B","op":"notContains","value":"某关键字"}]}
          ]
        }
        规则：
        - execution：必填。取值为 "sequence"（严格顺序，有依赖或条件时建议用此）、"parallel_waves"、"supervisor"。默认 "parallel_waves"。
        - id：唯一标识，建议 A、B、C 等。
        - question：该子问题的完整描述。
        - dependsOn：该任务依赖的任务 id 数组。空数组表示可立即执行；非空表示需等所列任务全部完成后再执行。
        - condition：可选，单个条件。仅当该条件在 scope 上成立时才执行该任务。sourceKey 为前置任务的 id（如 "A"），表示读取该任务在 scope 中的结果；op 取 contains/equals/notContains/present/absent；value 为比较用的字符串（present/absent 可空）。
        - conditions：可选，多个条件；全部满足才执行该任务。结构与 condition 相同，为数组。用于「仅当前置结果同时满足多条判断时才执行」的场景。
        - 有「先 A 后 B、且 B 是否执行依赖 A 的结果」时，用 execution:"sequence"，B 的 dependsOn:["A"]，并按业务含义配置 condition 或 conditions（sourceKey/op/value 由业务决定，不要写死具体领域）。
        - 若 B 依赖 A 且根据 A 的结果决定是否执行：除业务含义上的条件外，若 A 可能因无法完成而返回「失败/不可用」类说明（如含「无法」「不能」「失败」等），则 B 应增加「A 的结果不包含该类关键词」的条件（用 conditions 列出多条），以免 A 未成功时仍执行 B。具体关键词由 A 的语义与常见表述推断。
        - 若问题本身只对应一个简单任务，也输出仅含一个任务的数组，execution 仍必填。
        - 不绑定任何具体业务领域，通用拆分；condition/conditions 的 value 与判断逻辑完全由用户意图推断，勿穷举具体场景。
        """)
    @UserMessage("可执行问题：{{executableQuestion}}")
    String plan(String executableQuestion);
}
