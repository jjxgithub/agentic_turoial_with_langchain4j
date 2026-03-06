package com.cnblogs.yjmyzz.langchain4j.study.config;

import dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    public static OpenAiChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey("sk-9cf5412b286e4c05be1e6a849da44448")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .modelName("qwen-max")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .temperature(0.7)
                .build();
    }

    @Bean("azureOpenAiEmbeddingModel")
    public AzureOpenAiEmbeddingModel embeddingModel() {
        return AzureOpenAiEmbeddingModel.builder()
                .apiKey("9ae745aff97944b38e01bd1adfbaaab4")
                .endpoint("https://ai-server01.openai.azure.com/")
                .deploymentName("ai-server-text-embedding-ada-002")
                .timeout(Duration.ofSeconds(60))
                .logRequestsAndResponses(true)
                .build();
    }

}
