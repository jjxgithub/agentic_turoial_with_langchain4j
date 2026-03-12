# Skill 内整合 Agent 与 agentic 执行 — 设计说明

本文档整理「识别完 skill 后，在 skill 中整合 Agent，并用 langchain4j-agentic 执行步骤」的思路、正确性分析、通用性/扩展性，以及具体设计要点。**仅作设计说明，不涉及代码修改。**

---

## 一、思路正确性分析

**结论：方向正确。**

1. **分层清晰**
   - **外层**：补全 → 拆分 → 编排 → **按子问题匹配 skill**（谁来做）。
   - **内层**：命中 skill 后，**在 skill 内部**用「指定 Agent + 步骤」跑子流程（怎么做）。
   - 路由与执行分离，符合常见编排设计。

2. **用 agentic 跑 skill 内步骤**
   - 多步顺序执行 → 使用 **sequence** 很自然。
   - 步骤间需要传语义解析结果、意图、对齐结果等 → 使用 **AgenticScope** 做共享状态，配合 JSON 解析与引用，与 langchain4j-agentic 的用法一致。
   - 每步对应一个 Agent（或一个 Agent 多步不同 prompt），由 agentic 统一驱动。

3. **「在 skill 里指定用哪个 Agent、步骤是什么」**
   - 在 skill 定义（md/配置）中声明步骤与 Agent，或在代码中为每个 skill 注册一个「子工作流」。
   - 执行时：命中 skill → 取该 skill 的子工作流 → 用 agentic 执行。
   - 逻辑闭环，无明显漏洞。

**注意点**：若 skill 内步骤**依赖上游 task 的结果**（例如报表 skill 的输入要结合前面某个 task 的 scope），需在调用 `handle(question, plan)` 时把**当前 scope/上下文**传入，或在 agentic 子工作流入口将 `question` 与依赖结果写入 scope。约定好「skill 子工作流的输入从 scope 的哪几个 key 读」即可。

---

## 二、通用性分析

**结论：通用性较好，可覆盖多种 skill 形态。**

| 维度 | 说明 |
|------|------|
| **简单 skill** | 如打招呼、告别：子工作流仅 1～2 步、一个 Agent；与「多步报表」共用同一套「skill 内 agentic 子工作流」模型。 |
| **复杂 skill** | 报表 4 步、甚至更多步（如再加审批、通知）：同一套「步骤 + Agent + scope 传参」，仅步骤列表更长或中间加 condition/并行。 |
| **步骤形态** | 纯顺序 → sequence；某步失败走降级 → 某步后接 conditional；某几步可并行 → parallel 再接 sequence，均在 agentic 能力内。 |
| **与现有流水线** | 上层仍是「按子问题匹配 skill → 调 handler」；handler 内部是「用 agentic 跑本 skill 的步骤」。未命中 skill 的 task 继续走通用 Agent，无需为报表单独开一条管道。 |

---

## 三、扩展性分析

**结论：扩展性良好。**

- **加新 skill（新领域）**：新增 skill 的 md/配置 + 对应 Handler；Handler 内引用「预定义子工作流」或从 skill 定义中读取「步骤 + Agent」列表并用 agentic 构建执行。路由层无需改动。
- **在已有 skill 内加步骤或改顺序**：若步骤与 Agent 为**配置/声明式**（如写在 md 或 JSON），改配置并重新加载即可；若在代码中写死（如报表 4 步），仅改该 skill 的构建子工作流逻辑，其它 skill 不受影响。
- **新 Agent 接入**：新 Agent 满足「输入/输出通过 scope 或约定 key 读写」，即可作为 skill 内某一步挂入 sequence。
- **可选进一步扩展**：skill 内子工作流模板化（如「多步 + 每步一个 handlerKey」）；步骤间协议统一（如每步输出写 `stepId_result`、下一步从 scope 读），便于新增步骤。

---

## 四、整体分层（回顾）

- **外层（已有）**：补全 → 拆分 → 编排 → 按**子问题**匹配 skill → 命中则调 `SkillHandler.handle(question, plan)`，未命中走通用 Agent。
- **内层（设计目标）**：进入某 skill 后，该 skill 内部是「**一组步骤 + 每步用哪个 Agent**」，用 **langchain4j-agentic** 的 sequence（及可选的 condition/parallel）执行，步骤间用 **AgenticScope** 传数据。

---

## 五、Skill 内「步骤 + Agent」的表示方式

目标：每个 skill 能声明「内部有几步、每步叫什么、用哪个 Agent」，既支持报表 4 步等固定流程，又便于后续新增 skill 与步骤。

**两种层次（可并存）：**

1. **声明层（可配置、可扩展）**
   - 在 skill 的 **md front matter** 或单独配置中，用结构化方式列出「步骤列表」。
   - 每步至少包含：**步骤 id**、**步骤名**（如语义解析/意图提取/对齐/报表解析）、**对应 Agent 的标识**（如 handlerKey / agentId）。
   - 可选：步骤间依赖（默认顺序）、或简单条件（如「仅当上一步输出含某 key 才执行下一步」）。
   - 报表查询 = 4 步 + 4 个 Agent 标识；打招呼 = 2 步；告别 = 1 步，同一套模型通用。

2. **执行层（与 agentic 一一对应）**
   - 每个「步骤」在运行时对应 agentic 的一个 **子 Agent**（或 agentAction + Agent）。
   - 「用哪个 Agent」在**运行时**通过 **Agent 注册表** 解析：步骤里的 agentId/handlerKey → 实际执行体（如某 AiServices 接口实例或已封装的 RunnableStep）。
   - 新 Agent = 在注册表挂一个实现；新 skill = 新 md/配置 + 新 Handler，Handler 用「步骤列表 + 注册表」拼出子工作流。

---

## 六、用 agentic 构建并执行 skill 内子工作流

目标：skill 命中后，用 **AgenticServices.sequenceBuilder()**（或等价 API）将「步骤列表」变成一条可执行链，一次 invoke 得到最终结果。

**要点：**

1. **输入来源**
   - 子工作流入口输入 = 当前 task 的 **question**（及可选的 plan 摘要或依赖任务结果）。
   - invoke 前将这些写入 **AgenticScope**（如 `inputQuestion`、`contextFromDependencies`），第一步 Agent 从 scope 读取，与现有 GenericTaskAgent 用法一致。

2. **每一步在 agentic 中的形态**
   - 每步 = **agentAction（写 scope）** + **Agent（读 scope、计算、写回 scope）**。
   - agentAction：从 scope 整理「当前步输入」，写入当前步的 input key（如 `currentStepInput` 或 `step_语义解析_input`）。
   - Agent：从 scope 读输入，执行（LLM/工具等），将结果写回 scope（如 `step_语义解析_result` 或统一约定 `stepId`）。
   - 下一步的 agentAction 再从 scope 读上一步 result，整理成当前步 input，再调用当前步 Agent。
   - 步骤间数据流完全由 scope 的 key 约定与每步的读/写完成，与 PlanInterpreter 中「先 scopeSetter 再 taskAgent」的模式一致。

3. **最后一步的输出 = skill 返回值**
   - sequence 的 **output** 从 scope 取「最后一步写的结果」对应 key，作为 `handle(question, plan)` 的返回值，交回上层（如 SkillAwarePipelineService 中该 task 的 result）。

4. **若某 skill 需要条件分支**
   - 在步骤列表中可为某步声明「条件」（如依赖上一步某字段）。
   - 构建 sequence 时在该步前插入 agentic 的 **conditionalBuilder**：条件从 scope 读上一步结果并求值，再决定执行哪个子 Agent 或跳过。
   - 「skill = 一段 agentic 子工作流」的抽象不变，仅子工作流内包含 conditional。

---

## 七、步骤间数据与「JSON + 引用」

- **每步输出**：该步 Agent 的结果建议**结构化**（如 JSON），写入 scope 的约定 key（如 `step_意图提取_result`）。
- **下一步输入**：下一步的 agentAction 或 Agent 从 scope 读上一步 key，**解析 JSON**，按需取字段，再拼成当前步的 prompt/工具参数；需「引用」上一步某字段时，约定 key 名（如 `intent`、`alignedQuery`），从 JSON 中取。
- **通用 key 约定**：如 `step_{stepId}_result`、`step_{stepId}_input`，或统一用 `previousStepResult` / `currentStepInput`，便于通用模板与扩展。

---

## 八、与现有组件的衔接

- **SkillHandler.handle(question, plan)**
  - 入参不变：当前子问题的 **question**、当前 task 的 **singleTaskPlan**。
  - 在「报表查询」等 Handler 内：用 question（及 plan 若需要）作为子工作流**入口输入**写入 scope，再构建并 invoke 该 skill 的 **agentic 子工作流**（如 4 步 sequence），最后将 scope 中最后一步结果作为 return。
  - 调用方（如 SkillAwarePipelineService）无需改：仍为 `skill.handler().handle(taskQuestion, singleTaskPlan)`，仅 handler 内部从「手写多步调用」改为「步骤列表 + Agent 注册表 + agentic 执行」。

- **Agent 注册表**
  - 可为 **Map<agentId, Agent 实例>**（或能产出 Agent 的工厂），启动时注册：如 `semantic_parse` → 语义解析 Agent，`intent_extract` → 意图提取 Agent 等。
  - 报表 skill 的步骤声明中写 `agentId: semantic_parse` 等，构建 sequence 时从注册表取对应 Agent 挂到对应步。
  - 新 Agent = 实现接口/类 + 在注册表注册；新 skill = 新步骤列表 + 新 Handler 中复用「从步骤列表构建 sequence」的逻辑（或共用通用 SkillWorkflowRunner）。

- **与 PlanInterpreter 的关系**
  - PlanInterpreter 继续负责**外层** plan 的解析、拓扑排序，以及「未命中 skill 的 task」的通用 Agent 执行。
  - Skill 内部是**另一段** agentic 工作流，不经过 PlanInterpreter，由 skill 自身的「步骤 + 注册表」构建的 sequence 执行。两段工作流仅共享「都用 AgenticScope 传数据、都用 ChatModel」等基础设施，不共享具体步骤定义。

---

## 九、扩展性小结

| 场景 | 做法 |
|------|------|
| 加新 skill | 新 md/配置（步骤列表 + handlerId）+ 新 Handler；Handler 内复用「通用步骤执行器」或单独建 sequence；路由层不变。 |
| 改某 skill 的步骤 | 改该 skill 的步骤声明或该 Handler 内构建 sequence 的代码，其它 skill 不受影响。 |
| 加新步骤类型 / 新 Agent | 在 Agent 注册表增加一项；步骤声明中引用新 agentId。 |
| 步骤间协议统一 | 约定每步读写的 scope key（如 `step_{id}_result`）及 JSON 结构，新步骤遵守约定即可接入任意 skill。 |

---

## 十、总结：一句话思路

- **Skill 内**：用「步骤列表（id/name/agentId）+ Agent 注册表」描述「用哪个 Agent、顺序如何」。
- **执行**：用该描述 **构建 agentic 的 sequence**（入口将 question 写入 scope，每步 agentAction + Agent，步骤间通过 scope 的 JSON 结果与引用传参）。
- **出口**：最后一步写入 scope 的结果作为 `handle(question, plan)` 的返回值。
- **通用与扩展**：新 skill = 新声明 + 新 Handler；新步骤/新 Agent = 新注册表项 + 声明中引用；与现有「按子问题匹配 skill」的外层流程衔接，无需改动现有调用方代码，仅在对应 skill 的 Handler 内按上述思路实现内部执行即可。
