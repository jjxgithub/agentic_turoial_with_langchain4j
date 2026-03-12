# agentic_26_03_11 模块说明

基于 **Skill 发现 + 多步 Agent 编排** 的通用流水线：问题补全 → 子问题拆分 → 按子问题匹配 Skill → 命中则交该 Skill 的多步 Agent 执行，未命中走通用 Agent。

---

## 一、整体架构

```
                    ┌─────────────────────────────────────────────────────────────┐
                    │                    SkillAwarePipelineService                  │
                    │  补全 → 拆分(Plan) → 按 task 匹配 Skill → Handler.handle()   │
                    └─────────────────────────────────────────────────────────────┘
                                              │
           ┌──────────────────────────────────┼──────────────────────────────────┐
           │                                  │                                  │
           ▼                                  ▼                                  ▼
   ┌───────────────┐                 ┌───────────────┐                 ┌───────────────┐
   │ Skill 命中    │                 │ Skill 命中    │                 │ 未命中        │
   │ greeting      │                 │ report_query  │                 │ 通用 Agent    │
   └───────┬───────┘                 └───────┬───────┘                 └───────┬───────┘
           │                                  │                                  │
           ▼                                  ▼                                  ▼
   GreetingSkillHandler            ReportQuerySkillHandler              GenericTaskAgent
   (2 步: 语言→问候)                SkillWorkflowRunner.run(steps)       .execute(question)
                                           │
                                           ▼
                                   ┌───────────────────┐
                                   │ StepDef 列表      │ ← skills/steps/report_query.json
                                   │ 语义→意图→对齐→解析│   或 defaultSteps()
                                   └─────────┬─────────┘
                                             │
                                   ┌─────────▼─────────┐
                                   │ SkillWorkflowRunner│  sequence: 每步 = 前处理 + SubAgent + 后处理
                                   │ buildSequence()    │  scope: currentStepInput / step_*_result
                                   └───────────────────┘
```

- **入口**：`SkillAwarePipelineService.chat()` → 补全、拆 plan、对每个 task 做 `skillRegistry.findMatch(question)`。
- **Skill 定义**：`src/main/resources/skills/*.md`（YAML front matter：id、name、description、handlerId、keywords）。
- **步骤定义**：可选 `src/main/resources/skills/steps/{handlerId}.json`（StepDef 列表），未配置则用 Handler 内置 defaultSteps()。
- **平台层**：`skill.agentic` 包内 `SubAgentRegistry`、`StepProcessorRegistry`、`ToolRegistry`、`SkillWorkflowRunner`、`StepDefLoader` 与具体 skill 解耦。

---

## 二、目录与职责

| 路径 / 包 | 职责 |
|-----------|------|
| **根包** `agentic_26_03_11` | 流水线入口、Plan 解析、Task 编排、常量；Skill 未命中时通用 Agent。 |
| **skill** | Skill 发现与路由：Skill、SkillRegistry、SkillMatcher（LLM/Keyword）、SkillHandler、SkillHandlerRegistry、SKILL.md 加载。 |
| **skill.agentic** | 平台层：StepDef、SubAgent、SkillWorkflowRunner、StepDefLoader、ToolRegistry、GroupTool、重试/超时。 |
| **skill.agentic.report** | 报表查询 Skill：4 个 SubAgent、4 个 StepProcessor、ReportQuerySkillHandler、ReportAgenticConfig。 |
| **skill.demo** | Demo Skill：打招呼/告别 Handler 与 Step。 |
| **tools** | 供 step 挂载的 Tool（如 RelativeTimeResolverTool、WeatherQueryTool311）。 |

---

## 三、流水线 5 步（概要）

1. **问题补全**：`QuestionReformulator` 产出可执行问题。
2. **子问题拆分**：`PlanPlanner` 输出 JSON（execution + tasks），解析为 `PlanSchema`。
3. **计划解释**：`PlanInterpreter` 拓扑排序、依赖与条件求值（可选）。
4. **按 task 执行**：对每个 task 的 question 做 `skillRegistry.findMatch(question)`；命中则 `skill.handler().handle(question, singleTaskPlan)`，未命中则 `directTaskAgent.execute(question, context)`。
5. **Skill 内执行**：Handler 内多为 `SkillWorkflowRunner.run(steps, question)`，steps 来自 JSON 或 defaultSteps()，Runner 用 langchain4j-agentic 的 sequence 按步执行（每步可带前/后处理、Tool、重试、超时）。

---

## 四、Skill 定义（md）

- **位置**：`src/main/resources/skills/*.md`。
- **格式**：YAML front matter + 正文（可选）。必填：`id`、`handlerId`；建议：`name`、`description`、`keywords`。
- **加载**：启动时 `SkillMarkdownLoader.loadFromClasspath("classpath*:skills/*.md", skillHandlerRegistry)`，按 handlerId 绑定 Handler，注入 `SkillRegistry`。
- **匹配**：默认 `LlmSkillMatcher`（LLM 根据技能列表与用户问题返回 skill id）；可切换 `KeywordSkillMatcher`。

详见 [skill/README.md](skill/README.md)。现有 skill：`greeting`、`farewell`、`report_query`。

---

## 五、步骤配置（JSON，可选）

- **位置**：`src/main/resources/skills/steps/{handlerId}.json`，例如 `report_query.json`。
- **格式**：`{ "steps": [ { "id", "name", "agentId", "preProcessorId?", "postProcessorId?", "catchBeforeStepError?", "catchAgentError?", "catchAfterStepError?", "agentRetryCount?", "toolIds?", "stepTimeoutMs?" } ] }`。
- **约定**：`stepTimeoutMs` 为 -1 表示不超时；agentId / preProcessorId / postProcessorId 须在对应 Registry 中注册。

---

## 六、入口与事件流

- **REST**：`POST /api/agentic_26_03_11/skill-aware/stream`  
  请求体：`{"sessionId":"xxx","input":"用户输入"}`。  
  响应（SSE）：`intent_clear` → `plan` → 对每个 task：`task_start` →（若命中）`skill_matched` → `task_result` → `task_end` → … → `plan_done`。

---

## 七、相关文档

- [skill/README.md](skill/README.md)：Skill 发现与路由、SKILL.md 约定、扩展新 Skill。
- [AGENT_DEVELOPMENT.md](AGENT_DEVELOPMENT.md)：与报表分析同类的 Agent 开发流程与架构图（SubAgent、StepProcessor、步骤 JSON、配置注册）。
