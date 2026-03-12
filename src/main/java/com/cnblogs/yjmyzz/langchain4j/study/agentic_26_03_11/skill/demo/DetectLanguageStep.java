package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Demo Agent 内部步骤1：从用户输入识别使用语言（如 中文、English）。
 */
public interface DetectLanguageStep {

    @SystemMessage("根据用户输入判断其使用的主要语言，只输出语言名称，如：中文、English、日本語。不要其他解释。")
    @UserMessage("用户输入：{{userInput}}")
    String detect(@V("userInput") String userInput);
}
