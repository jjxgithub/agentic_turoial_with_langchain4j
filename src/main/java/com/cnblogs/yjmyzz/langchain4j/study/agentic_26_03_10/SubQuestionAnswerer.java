package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 针对单个子问题作答，可带入依赖任务的结果作为上下文。
 */
public interface SubQuestionAnswerer {

    @SystemMessage("""
        你根据「依赖任务的结果」作为上下文，回答「当前子问题」。
        若没有依赖上下文，则仅根据子问题本身作答。回答简洁、准确。
        """)
    @UserMessage("""
        依赖任务的结果（若无则忽略）：
        {{contextFromDependencies}}
        
        当前子问题：{{question}}
        """)
    String answer(String question, String contextFromDependencies);
}
