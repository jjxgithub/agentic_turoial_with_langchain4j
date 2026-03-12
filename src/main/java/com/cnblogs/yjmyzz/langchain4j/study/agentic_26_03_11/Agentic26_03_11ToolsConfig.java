package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools.EmailNotifyTool311;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools.WeatherQueryTool311;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 单 Agent + 全量工具 模式所需 Bean：UnifiedTaskAgentWithTools（带工具）供 PlanInterpreterWithTools 使用。
 */
@Configuration
public class Agentic26_03_11ToolsConfig {

    /** 供 parallel_waves 内直接调用及 agentic 构建时复用同一套工具。 */
    @Bean
    public UnifiedTaskAgentWithTools unifiedTaskAgentWithToolsForDirectCall(
            ChatModel chatModel,
            WeatherQueryTool311 weatherQueryTool311,
            EmailNotifyTool311 emailNotifyTool311) {
        return AiServices.builder(UnifiedTaskAgentWithTools.class)
                .chatModel(chatModel)
                .tools(weatherQueryTool311, emailNotifyTool311)
                .build();
    }
}
