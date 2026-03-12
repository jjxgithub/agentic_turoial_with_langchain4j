package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.GreetingSkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.FarewellSkillHandler;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Skill 发现与 Demo 配置：Handler 按 handlerId 注册，Skill 从 SKILL.md 加载后与 Handler 绑定。
 */
@Configuration
public class SkillDemoConfig {

    @Bean
    public com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.DetectLanguageStep detectLanguageStep(ChatModel chatModel) {
        return AiServices.builder(com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.DetectLanguageStep.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.GenerateGreetingStep generateGreetingStep(ChatModel chatModel) {
        return AiServices.builder(com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.GenerateGreetingStep.class)
                .chatModel(chatModel)
                .build();
    }

    @Bean
    public com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.FarewellReplyStep farewellReplyStep(ChatModel chatModel) {
        return AiServices.builder(com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.FarewellReplyStep.class)
                .chatModel(chatModel)
                .build();
    }

    /** handlerId → SkillHandler，供 SKILL.md 中的 handlerId 解析。 */
    @Bean
    public SkillHandlerRegistry skillHandlerRegistry(
            GreetingSkillHandler greetingSkillHandler,
            FarewellSkillHandler farewellSkillHandler) {
        return new SkillHandlerRegistry()
                .register("greeting", greetingSkillHandler)
                .register("farewell", farewellSkillHandler);
    }

    /** 从 classpath:skills/*.md 加载 skill 定义，并绑定 Handler。 */
    @Bean
    public SkillRegistry skillRegistry(SkillHandlerRegistry skillHandlerRegistry) {
        List<Skill> fromMd = SkillMarkdownLoader.loadFromClasspath("classpath*:skills/*.md", skillHandlerRegistry);
        SkillRegistry registry = new SkillRegistry();
        registry.registerAll(fromMd);
        return registry;
    }
}
