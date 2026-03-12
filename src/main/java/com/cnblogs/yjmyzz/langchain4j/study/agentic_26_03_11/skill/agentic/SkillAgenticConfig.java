package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools.RelativeTimeResolverTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通用 Skill + SubAgent 编排配置：注册 SubAgentRegistry、StepProcessorRegistry、ToolRegistry 与 SkillWorkflowRunner，供各 skill 注入并注册步骤 Agent 及可选前/后处理、按 StepDef.toolIds 挂载 tools。
 */
@Configuration
public class SkillAgenticConfig {

    @Bean
    public SubAgentRegistry subAgentRegistry() {
        return new SubAgentRegistry();
    }

    @Bean
    public StepProcessorRegistry stepProcessorRegistry(DemoStepProcessor demoStepProcessor) {
        return new StepProcessorRegistry().register(Agentic311Constants.Demo.PROCESSOR_ID, demoStepProcessor);
    }

    @Bean
    public ToolRegistry toolRegistry(RelativeTimeResolverTool relativeTimeResolverTool) {
        return new ToolRegistry().register(Agentic311Constants.ToolIds.RELATIVE_TIME_RESOLVER, relativeTimeResolverTool);
    }

    /** 步骤配置外置：从 classpath:skills/steps/{skillId}.json 加载，供各 skill handler 使用。 */
    @Bean
    public StepDefLoader stepDefLoader(ObjectMapper objectMapper) {
        return new StepDefLoader(objectMapper);
    }

    /** Step/Workflow 超时用的线程池，应用关闭时自动 shutdown（destroyMethod）。 */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService skillStepTimeoutExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "skill-step-timeout");
            t.setDaemon(false);
            return t;
        });
    }

    @Bean
    public SkillWorkflowRunner skillWorkflowRunner(
            ChatModel chatModel,
            SubAgentRegistry subAgentRegistry,
            StepProcessorRegistry stepProcessorRegistry,
            @Autowired(required = false) SubAgentInstanceRegistry subAgentInstanceRegistry,
            @Autowired(required = false) ToolRegistry toolRegistry,
            @Autowired(required = false) ExecutorService skillStepTimeoutExecutor) {
        return new SkillWorkflowRunner(chatModel, subAgentRegistry, stepProcessorRegistry, subAgentInstanceRegistry, toolRegistry, skillStepTimeoutExecutor);
    }
}
