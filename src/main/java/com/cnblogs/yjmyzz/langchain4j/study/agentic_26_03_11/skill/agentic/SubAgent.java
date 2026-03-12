package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import dev.langchain4j.service.V;

/**
 * Skill 内单步 Agent 的通用契约：从 scope 的 currentStepInput 读入，返回结果由编排层写入 scope。
 * 各领域定义自己的 Agent 接口（含 @Agent / @SystemMessage / @UserMessage），方法签名为本方法即可，由 SubAgentRegistry 注册、SkillWorkflowRunner 编排。
 */
public interface SubAgent {

    String execute(@V("currentStepInput") String currentStepInput);
}
