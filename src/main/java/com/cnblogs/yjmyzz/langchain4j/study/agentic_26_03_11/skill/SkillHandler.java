package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.PlanSchema;

import java.util.Map;

/**
 * Skill 对应的执行体：由具体 Agent 实现，内部可包含多步（如语义解析→意图提取→对齐→报表解析）。
 * 编排层在 skill 命中后调用此接口，不再执行通用 plan。
 * 支持三参 handle：传入前置任务结果与 plan 中的 skillStepHint，供多步 Skill 按子步骤执行（方案四）。
 */
public interface SkillHandler {

    /**
     * 使用本 skill 的 Agent 处理请求。
     *
     * @param executableQuestion 补全后的可执行问题
     * @param plan               已拆分的计划（含当前任务及可选的 skillStepHint）
     * @return Agent 内部多步执行后的最终结果
     */
    String handle(String executableQuestion, PlanSchema plan);

    /**
     * 同上，并传入前置任务 id → 结果的映射，便于多步 Skill 根据 skillStepHint 只跑子步骤并用前置结果作为首步输入。
     * 默认实现忽略 previousTaskResults，调用 {@link #handle(String, PlanSchema)}。
     *
     * @param previousTaskResults 当前任务依赖的前置任务 id 到其执行结果的映射，可能为空
     * @return Agent 执行结果
     */
    default String handle(String executableQuestion, PlanSchema plan, Map<String, Object> previousTaskResults) {
        return handle(executableQuestion, plan);
    }
}
