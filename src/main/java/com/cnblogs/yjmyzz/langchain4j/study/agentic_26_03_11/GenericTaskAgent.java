package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 通用任务执行 Agent：根据当前子问题与依赖上下文作答，结果由编排层写入 scope（outputKey 由解释器指定）。
 * 不绑定具体领域，所有子任务共用此 Agent。
 */
public interface GenericTaskAgent {

    @Agent("根据当前子问题与依赖上下文作答，输出简洁结果")
    @SystemMessage("""
        你根据「依赖任务的结果」作为上下文，回答「当前子问题」。
        若没有依赖上下文，则仅根据当前子问题本身作答。回答简洁、准确。不绑定任何具体业务领域。
        """)
    @UserMessage("""
        依赖任务的结果（若无则忽略）：
        {{contextFromDependencies}}
        
        当前子问题：{{currentQuestion}}
        """)
    String execute(@V("currentQuestion") String currentQuestion, @V("contextFromDependencies") String contextFromDependencies);
}
