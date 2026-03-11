package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为子问题拆分与编排提供 PlanPlanner、SubQuestionAnswerer、ObjectMapper 等 Bean。
 */
@Configuration
public class Agentic26_03_10Config {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

//    @Bean
//    public PlanPlanner planPlanner(ChatModel chatModel) {
//        return AiServices.builder(PlanPlanner.class)
//                .chatModel(chatModel)
//                .build();
//    }

    @Bean
    public SubQuestionAnswerer subQuestionAnswerer(ChatModel chatModel) {
        return AiServices.builder(SubQuestionAnswerer.class)
                .chatModel(chatModel)
                .build();
    }
}
