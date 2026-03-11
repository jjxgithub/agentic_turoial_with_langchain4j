# agentic_26_03_11：通用 5 步流水线（澄清 → 补全 → 拆分 → 计划 → agentic 执行）

与 `agentic_26_03_10` 同级别，不绑定具体业务领域。**完整支持 sequence、parallel_waves、conditional、supervisor**。

## 5 步流程

1. **缺条件分析**：`ClarificationAnalyzer` 结合当前 input 与对话记忆，输出 `NEED_CLARIFICATION` 或 `PROCEED`。
2. **问题补全**：`QuestionReformulator` 在保留用户原文前提下，用记忆补全成一条可执行问题。
3. **子问题拆分**：`PlanPlanner` 输出 JSON，包含 `execution` 与 `tasks`（每任务可有 `condition`），解析为 `PlanSchema`。
4. **计划解释**：`PlanInterpreter` 根据 `execution` 与任务依赖/条件，构建 agentic 工作流。
5. **执行**：运行对应工作流，结果写回 scope 或返回摘要。

## 计划 Schema

- **PlanSchema**  
  - `tasks`: 子任务列表。  
  - `execution`: `"sequence"` | `"parallel_waves"` | `"supervisor"`（默认 `parallel_waves`）。

- **TaskSchema**  
  - `id`, `question`, `dependsOn`（依赖的 taskId 列表）。  
  - `condition`（可选）：`{ "sourceKey": "A", "op": "contains"|"equals"|"notContains"|"present"|"absent", "value": "雨" }`，在 scope 上求值，成立才执行该任务。

## 执行模式与 agentic 映射

| execution        | 行为 | agentic 使用 |
|------------------|------|----------------|
| **sequence**     | 严格按拓扑序执行，带条件的任务用条件分支 | `sequenceBuilder` + 每步 `agentAction`(写 scope) + 可选 `conditionalBuilder`(condition, agent) |
| **parallel_waves** | 按层执行：同层多任务在同一 wave 内顺序执行（共享 scope），层间顺序；单任务层走 sequence 步 | 同层多任务：`agentAction` 写各 task 的 question_/context_，再 `agentAction` 内用 `directTaskAgent.execute` 顺序写回 scope；单任务同 sequence |
| **supervisor**   | 监督者动态选择下一个子任务 | `supervisorBuilder`，子 agent 为 `sequence(setScope, genericAgent)`，返回摘要（无 per-task scope） |

## 条件求值（ConditionSchema）

在 scope 上读取 `sourceKey` 对应值，再按 `op` 与 `value` 比较：

- `contains` / `notContains`：字符串包含/不包含。  
- `equals`：字符串相等。  
- `present` / `absent`：存在且非空 / 不存在或空（可省略 value）。

## 入口

- **REST**：`POST /api/agentic_26_03_11/chat/stream`  
  请求体：`{"sessionId":"xxx","input":"用户输入"}`  
  响应（SSE）：`clarification` / `intent_clear` → `plan` → `task_start` / `task_result` / `task_end` → `plan_done` 或 `error`。  
  supervisor 模式下 `plan_done` 为监督者摘要，无逐任务 result。
