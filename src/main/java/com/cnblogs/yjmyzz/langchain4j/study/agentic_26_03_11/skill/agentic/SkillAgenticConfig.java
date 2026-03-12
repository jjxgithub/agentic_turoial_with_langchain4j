package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通用 Skill + SubAgent 编排配置：注册 SubAgentRegistry、StepProcessorRegistry 与 SkillWorkflowRunner，供各 skill 注入并注册步骤 Agent 及可选前/后处理。
 */
@Configuration
public class SkillAgenticConfig {

    public static final String DEMO_PROCESSOR_ID = "demo";

    @Bean
    public SubAgentRegistry subAgentRegistry() {
        return new SubAgentRegistry();
    }

    @Bean
    public StepProcessorRegistry stepProcessorRegistry(DemoStepProcessor demoStepProcessor) {
        return new StepProcessorRegistry().register(DEMO_PROCESSOR_ID, demoStepProcessor);
    }

    @Bean
    public SkillWorkflowRunner skillWorkflowRunner(
            ChatModel chatModel,
            SubAgentRegistry subAgentRegistry,
            StepProcessorRegistry stepProcessorRegistry,
            @Autowired(required = false) SubAgentInstanceRegistry subAgentInstanceRegistry) {
        return new SkillWorkflowRunner(chatModel, subAgentRegistry, stepProcessorRegistry, subAgentInstanceRegistry);
    }
}
