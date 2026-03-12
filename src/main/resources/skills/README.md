# Skill 定义（Markdown）

本目录下每个 `.md` 文件对应一个 Skill，由 `SkillMarkdownLoader` 在启动时加载（`classpath*:skills/*.md`），解析 YAML front matter 后与 `SkillHandlerRegistry` 中的 Handler 按 `handlerId` 绑定，并注入 `SkillRegistry` 供流水线按子问题匹配。

---

## 文件格式

每个文件以 YAML front matter 开头（两个 `---` 之间），必填与可选字段如下。

| 字段 | 必填 | 说明 |
|------|------|------|
| **id** | 是 | Skill 唯一标识，与文件名无强制关系，但通常与 handlerId 一致。 |
| **handlerId** | 是 | 对应代码中 `SkillHandlerRegistry.register(handlerId, handler)` 的 key，用于解析 Handler。 |
| **name** | 否 | 展示名称，缺省用 id。 |
| **description** | 否 | 技能描述，供 LLM 路由与文档使用。 |
| **keywords** | 否 | 关键词列表（YAML 列表或逗号分隔），可用于关键词匹配或说明。 |

匹配策略默认使用 **LLM 路由**（`LlmSkillMatcher`）：将本目录加载得到的技能列表（id / name / description）与用户可执行问题一起交给 LLM，由 LLM 返回最匹配的 skill id 或 none。

---

## 当前 Skill 列表

| 文件 | id | handlerId | 说明 |
|------|-----|-----------|------|
| [greeting.md](greeting.md) | greeting | greeting | 打招呼：识别语言 → 生成问候。 |
| [farewell.md](farewell.md) | farewell | farewell | 告别：生成简短道别回复。 |
| [report_query.md](report_query.md) | report_query | report_query | 报表查询：语义解析 → 意图提取 → 对齐 → 报表解析（多步 SubAgent + 可选 steps JSON）。 |

---

## 步骤配置（可选）

若某 Skill 使用多步 SubAgent 编排，可额外在 **steps 子目录** 下为该 Skill 的 handlerId 配置步骤 JSON：

- 路径：`skills/steps/{handlerId}.json`
- 示例：`skills/steps/report_query.json`
- 格式与字段说明见模块文档 `agentic_26_03_11` 包下的 **AGENT_DEVELOPMENT.md** 与 **README.md**（步骤配置一节）。

未配置 JSON 时，对应 Handler 使用代码中的默认步骤（如 `ReportQuerySkillHandler.defaultSteps()`）。
