/**
 * Skill 内 Agent + SubAgent 通用编排（平台层）。
 * <p>
 * 与具体 skill 解耦：通过 {@link StepDef} 声明步骤、{@link SubAgentRegistry} 注册 SubAgent 类、
 * {@link SkillWorkflowRunner} 用 langchain4j-agentic 的 sequence 编排执行。
 * <p>
 * 扩展新 skill：在独立子包（如 {@code skill.agentic.report}）中定义步骤 Agent 接口、注册到
 * {@link SubAgentRegistry}、实现 {@link com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.SkillHandler}
 * 并调用 {@link SkillWorkflowRunner#run(List, String)}。
 */
package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;
