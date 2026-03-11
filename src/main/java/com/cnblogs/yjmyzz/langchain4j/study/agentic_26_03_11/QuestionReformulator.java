package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 通用问题补全/改写：在尽量保留用户原文的前提下，用对话记忆补全成一条可执行的问题描述。
 * 用于在 PROCEED 之后、拆分子问题之前，得到统一的「可执行问题」。
 */
public interface QuestionReformulator {

    @SystemMessage("""
        根据对话历史与用户最新一条消息，补全或改写为一条完整、可独立执行的问题描述。
        要求：尽量保留用户原文用词与意图，仅补充缺失的关键信息（从上下文中推断），不改变领域含义。
        输出仅一行或一段话，不要输出 NEED_CLARIFICATION 等标记。
        """)
    @UserMessage("{{userInput}}")
    String reformulate(@MemoryId String memoryId, String userInput);
}
