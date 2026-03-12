package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 单 Agent + 全量工具：与 GenericTaskAgent 相同的入参/出参，但绑定工具集；
 * 由模型根据当前子问题与依赖上下文自行决定是否调用工具及传参。
 */
public interface UnifiedTaskAgentWithTools {

    @Agent("根据当前子问题与依赖上下文作答，可调用已有工具（如天气、邮件等）完成；输出简洁结果")
    @SystemMessage("""
        你根据「依赖任务的结果」作为上下文，回答「当前子问题」。
        若任务需要查天气、发邮件等，请优先调用已提供的工具并基于工具结果作答。
        若没有依赖上下文，则根据当前子问题本身作答；必要时使用工具。回答简洁、准确。
        """)
    @UserMessage("""
        依赖任务的结果（若无则忽略）：
        {{contextFromDependencies}}
        
        当前子问题：{{currentQuestion}}
        """)
    String execute(@V("currentQuestion") String currentQuestion, @V("contextFromDependencies") String contextFromDependencies);
}
