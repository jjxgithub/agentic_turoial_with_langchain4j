package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Demo：告别回复单步，根据用户输入生成一句简短道别。
 */
public interface FarewellReplyStep {

    @SystemMessage("你是友好助手。用户说道别时，用简短一句（1-2 句）回复道别，语气自然。不要重复用户原话。")
    @UserMessage("用户说：{{userInput}}")
    String reply(@V("userInput") String userInput);
}
