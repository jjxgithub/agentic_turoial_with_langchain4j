---
id: report_query
name: 报表查询
description: 用户询问报表、数据统计、看板、指标时由本 Skill 处理，内部 4 步为语义解析 → 意图提取 → 对齐 → 报表解析，由 agentic SubAgent 编排执行。
handlerId: report_query
keywords:
  - 报表
  - 查询
  - 统计
  - 看板
  - 指标
  - 数据
---

# 报表查询

当用户输入与报表、数据统计、看板、指标相关时，由报表查询 Skill 处理。内部步骤：语义解析 → 意图提取 → 对齐 → 报表解析，使用 langchain4j-agentic 的 SubAgent 顺序执行。
