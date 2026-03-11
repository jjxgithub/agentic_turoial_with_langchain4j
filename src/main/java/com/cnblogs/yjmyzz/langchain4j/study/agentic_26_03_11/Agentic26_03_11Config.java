package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用 5 步流水线配置：记忆、ClarificationAnalyzer、QuestionReformulator、PlanPlanner。
 * ObjectMapper 使用应用已有的 Bean。
 */
@Configuration
public class Agentic26_03_11Config {

    private static final int MAX_MEMORY_MESSAGES = 20;

    @Bean
    public ChatMemoryProvider agentic26_03_11MemoryProvider() {
        Map<String, MessageWindowChatMemory> store = new ConcurrentHashMap<>();
        return memoryId -> store.computeIfAbsent(
                String.valueOf(memoryId),
                k -> MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES)
        );
    }

    @Bean
    public ClarificationAnalyzer clarificationAnalyzer(
            ChatModel chatModel,
            ChatMemoryProvider agentic26_03_11MemoryProvider) {
        return AiServices.builder(ClarificationAnalyzer.class)
                .chatModel(chatModel)
                .chatMemoryProvider(agentic26_03_11MemoryProvider)
                .build();
    }

    @Bean
    public QuestionReformulator questionReformulator(
            ChatModel chatModel,
            ChatMemoryProvider agentic26_03_11MemoryProvider) {
        return AiServices.builder(QuestionReformulator.class)
                .chatModel(chatModel)
                .chatMemoryProvider(agentic26_03_11MemoryProvider)
                .build();
    }

    @Bean
    public PlanPlanner planPlanner(ChatModel chatModel) {
        return AiServices.builder(PlanPlanner.class)
                .chatModel(chatModel)
                .build();
    }

    /** 用于 parallel_waves 中同层多任务时在 Java 内直接调用，不经过 agentic 编排。 */
    @Bean
    public GenericTaskAgent genericTaskAgentForDirectCall(ChatModel chatModel) {
        return AiServices.builder(GenericTaskAgent.class)
                .chatModel(chatModel)
                .build();
    }
}
