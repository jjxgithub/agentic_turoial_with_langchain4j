# 监督者编排（_7b）性能优化方案

## 一、问题简述

- **总耗时**：~236 秒，约 14+ 次 LLM 调用。
- **上下文**：每次规划都带上完整用户请求（简历、职位描述、HR 要求、电话记录等），且多轮对话累积，单次请求可达 8000+ tokens。
- **额外调用**：每步子 agent 执行后做 1 次摘要（共 4 次）；最后 SCORED 再做 1 次评分。

---

## 二、优化方案（按收益/难度排序）

### 1. 调整上下文策略（推荐，改 1 行）

**当前**：`CHAT_MEMORY_AND_SUMMARIZATION` → 每步都做摘要，规划时用「对话历史 + 摘要」。

**可选**：

| 策略 | 效果 | 适用场景 |
|------|------|----------|
| **CHAT_MEMORY** | 去掉 4 次摘要调用，总耗时明显下降；规划时用完整对话，单轮 token 可能更大 | 对话轮数不多（如本次 4 个子 agent），可优先试 |
| **SUMMARIZATION** | 规划时只用摘要，不用完整对话，单轮上下文更短；仍会有摘要调用 | 轮数很多、怕超长上下文时 |

**建议**：先改为 `SupervisorContextStrategy.CHAT_MEMORY`，在 _7b 这种 4 步链路上通常能少 4 次调用、总时长可降约 30–60 秒，且不增加单轮长度到不可接受。

```java
.contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY)  // 去掉每步摘要，减少 4 次调用
```

---

### 2. 简化最终响应策略（推荐，改 1 行）

**当前**：`SCORED` → 多 1 次「对两种回复打分」的 LLM 调用。

若业务上不需要在「最后一句话」和「监督者总结」之间自动二选一，可改为：

- **LAST**：直接返回最后一个子 agent 的回复（如 organize 的面试安排说明），少 1 次调用。
- **SUMMARY**：直接返回监督者总结，也少 1 次评分调用。

```java
.responseStrategy(SupervisorResponseStrategy.LAST)   // 或 SUMMARY，省 1 次评分
```

---

### 3. 缩短传入的 request（中等收益，需改 request 构建）

**现状**：`request` 由「指令 + 简历全文 + 联系方式 + 职位描述全文 + HR 要求全文 + 电话记录全文」拼接，是单次请求里 token 最多的部分，且会在多轮中被重复带入。

**思路**：在不影响子 agent 能力的前提下，尽量「一次注入、多次引用」，或只传必要摘要。

- **方案 A（推荐）**：若 LangChain4j 支持在调用前向 `AgenticScope` 写入初始状态（如 `candidateCv`、`jobDescription` 等），则：
  - request 改为短指令 + 键名引用，例如：  
    `"评估此候选人并安排面试或拒绝。材料已注入 scope：candidateCv, candidateContact, jobDescription, hrRequirements, phoneInterviewNotes。"`
  - 子 agent 从 scope 按 key 读取长文本，监督者规划时不再在 arguments 里重复贴全文。  
  → 需确认框架是否支持「调用前写入 scope」以及子 agent 是否可从 scope 取参。

- **方案 B（无需改框架）**：对「仅给监督者看、子 agent 不需要全文」的部分做一次预摘要再放进 request。例如：
  - 职位描述：保留职责与要求要点，去掉大段格式/公司介绍。
  - 电话记录：保留结论性要点（是否通过、薪资/到岗等），去掉逐字稿。  
  这样规划阶段上下文变短，子 agent 仍通过现有参数拿到「完整简历」等（因为评审必须看全文）。可对 `jobDescription`、`phoneInterviewNotes` 做截断或本地用一次 cheap 摘要再拼接进 request。

---

### 4. 在 supervisorContext 里约束参数长度（辅助）

在现有 `supervisorContext` 里增加对「参数不要重复贴全文」的提示，例如：

```text
调用子智能体时，若某参数与用户请求中某段已出现内容一致，可写「同用户请求中的 candidateCv」等简短引用，避免在 arguments 中重复粘贴超长原文。
```

效果取决于框架是否允许子 agent 从「用户请求」或 scope 中解析这类引用；若框架会原样把 arguments 传给子 agent，而子 agent 又必须拿到全文，则这条只能作为补充，不能替代方案 3。

---

### 5. 监督者与摘要用更小/更快模型（若 API 支持）

- 规划（planner）和摘要（若保留）对「推理深度」要求低于具体评审，可换为更快或更便宜的模型，减少单次延迟与成本。
- 子 agent（HR/经理/团队评审、面试安排）保持强模型，保证质量。

需确认 LangChain4j 的 `supervisorBuilder` 是否支持为监督者单独指定 `ChatModel`，以及摘要是否使用同一模型配置。

---

### 6. 降低 temperature（可选）

对监督者或摘要模型将 `temperature` 调低（如 0.3），可能略微加快解码并减少无关发散，对耗时帮助较小，但有利于稳定顺序与格式。

---

## 三、推荐组合（最小改动、明显收益）

在 _7b 中先做两处修改：

1. **上下文策略**改为 `CHAT_MEMORY`，减少 4 次摘要调用。
2. **响应策略**改为 `LAST` 或 `SUMMARY`，减少 1 次 SCORED 调用。

预期：少 5 次 LLM 调用，总耗时有望从 ~236 秒降到约 150–180 秒量级；若再配合 request 缩短（方案 3），可进一步降低单轮 token 与整体延迟。

---

## 四、小结

| 方向 | 主要作用 | 改动量 |
|------|----------|--------|
| CHAT_MEMORY | 减少 4 次摘要、缩短总时长 | 1 行 |
| LAST/SUMMARY | 减少 1 次评分 | 1 行 |
| 缩短 request / scope 注入 | 降低上下文长度、重复 token | 中（request 构建或框架用法） |
| supervisorContext 提示 | 减少重复长参（若框架支持引用） | 少量文案 |
| 监督者/摘要用更快模型 | 降低单次调用延迟与成本 | 取决于 API 是否支持 |

优先做「上下文策略 + 响应策略」两处，再视需要做 request/上下文长度优化。
