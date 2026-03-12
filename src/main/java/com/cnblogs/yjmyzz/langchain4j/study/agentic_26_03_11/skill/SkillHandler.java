package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.PlanSchema;

/**
 * Skill 对应的执行体：由具体 Agent 实现，内部可包含多步（如语义解析→意图提取→对齐→报表解析）。
 * 编排层在 skill 命中后调用此接口，不再执行通用 plan。
 */
public interface SkillHandler {

    /**
     * 使用本 skill 的 Agent 处理请求。
     *
     * @param executableQuestion 补全后的可执行问题
     * @param plan               已拆分的计划（可选使用，如仅用摘要或不用）
     * @return Agent 内部多步执行后的最终结果
     */
    String handle(String executableQuestion, PlanSchema plan);
}
