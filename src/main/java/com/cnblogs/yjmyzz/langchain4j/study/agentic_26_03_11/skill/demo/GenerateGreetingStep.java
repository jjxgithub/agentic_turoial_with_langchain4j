package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Demo Agent 内部步骤2：根据语言生成一句问候回复。
 */
public interface GenerateGreetingStep {

    @SystemMessage("你是一个友好的助手。根据用户输入和识别出的语言，用该语言写一句简短、自然的问候回复（1-2 句）。不要翻译用户原文，只输出你的问候。")
    @UserMessage("用户输入：{{userInput}}\n识别语言：{{language}}")
    String generate(@V("userInput") String userInput, @V("language") String language);
}
