# Skill 发现与路由（编排后由哪个 Agent 处理）

## 思路

- **大逻辑不变**：问题补全 → 子问题拆分 → 编排（plan）。
- **编排完成后**：用 **skill 列表**做发现（可结合 langchain-skill 规范），命中则交给该 skill 对应的 **Agent** 处理；该 Agent 内部可包含多步（如报表：语义解析 → 意图提取 → 对齐 → 报表解析）。
- **未命中**：仍按通用 plan 执行（现有 PlanInterpreter）。

## 通过 SKILL.md 注册

Skill 定义放在 `src/main/resources/skills/` 下，每个 `.md` 一篇，YAML front matter 约定：

```yaml
---
id: greeting
name: 打招呼
description: 用户打招呼、问好时由本 Agent 处理。
handlerId: greeting
keywords:
  - 你好
  - 嗨
  - hello
  - hi
---
```

- **id**：唯一标识。
- **name** / **description**：展示或后续扩展用。
- **handlerId**：对应代码里在 `SkillHandlerRegistry` 中注册的 handler（如 `greeting` → `GreetingSkillHandler`）。
- **keywords**：在 md 中保留用于展示或后续扩展；**匹配由 LLM 路由完成**，不依赖关键词包含。

**匹配策略（行业通用做法）**：默认使用 **LLM 路由**（`SkillRouter` + `LlmSkillMatcher`）：将技能列表（id / name / description）与用户可执行问题交给 LLM，由 LLM 返回最匹配的 skill id 或 none。可切换为 `KeywordSkillMatcher`（按 keywords 包含）用于回退或测试。

启动时 `SkillMarkdownLoader.loadFromClasspath("classpath*:skills/*.md", skillHandlerRegistry)` 加载所有 md，解析出 `Skill` 并注入 `SkillRegistry`；`SkillRegistry` 构造时注入 `SkillMatcher`（默认 `LlmSkillMatcher`）。

## Demo（非报表）

- **greeting**：`skills/greeting.md`，handlerId=greeting → `GreetingSkillHandler`（内部两步：识别语言 → 生成问候）。
- **farewell**：`skills/farewell.md`，handlerId=farewell → `FarewellSkillHandler`（单步：生成道别回复）。
- **接口**：`POST /api/agentic_26_03_11/skill-aware/stream`，Body：`{ "sessionId": "x", "input": "你好呀" }` 或 `"再见"`。
- **事件流**：clarification（如需）→ intent_clear → plan → **skill_matched**（命中的 skill id）→ plan_done。

## 扩展新 Skill（含报表）

1. 在 `src/main/resources/skills/` 下新增 `report_query.md`，写好 id、name、description、handlerId、keywords。
2. 实现 `SkillHandler`（如 `ReportQuerySkillHandler`），在 `handle(executableQuestion, plan)` 内跑四步。
3. 在 `SkillDemoConfig#skillHandlerRegistry` 中 `register("report_query", reportQuerySkillHandler)`。
4. 无需改加载逻辑，重启即可从 md 加载新 skill。
