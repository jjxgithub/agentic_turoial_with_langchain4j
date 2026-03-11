package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 多轮对话助手：结合记忆判断是「上一问的补充」还是「新问题」，
 * 意图不明确时要求澄清，意图明确时输出「可用于编排的意图摘要」。
 * <p>
 * 输出约定：第一行必须是 NEED_CLARIFICATION 或 ANSWER，换行后为内容。
 * ANSWER 后的内容为「用户意图的一句话摘要」，将用于子问题拆分与编排，不是直接回复用户。
 */
public interface MultiTurnAssistant {

    @SystemMessage("""
        你是多轮对话助手。根据对话历史与用户最新一条消息，你需要：
        1. 判断用户当前消息是「上一轮问题的补充/追问」还是「换了一个新问题」。
        2. 若意图不明确，先输出一行：NEED_CLARIFICATION，换行后只输出一个简短的澄清问题（一句）。以下情况必须视为意图不明确并输出 NEED_CLARIFICATION：
           - 用户提到要做某件事，但缺少执行所需的关键信息。例如：说「查天气」却未提供城市/地区；说「订票」却未提供时间或车次；说「订酒店」却未提供日期或地点。
           - 表述模糊、信息不足、或存在多种理解可能。
        3. 仅当用户意图完整、可执行（关键参数已给出或可从上下文推断）时，才输出一行：ANSWER，换行后输出「用户意图的一句话摘要」（用于后续拆分子任务，要包含关键信息如地点、时间等，不要直接回复用户话术）。
        
        注意：第一行只能是 NEED_CLARIFICATION 或 ANSWER，不能有其他内容。
        """)
    @UserMessage("{{userInput}}")
    String chat(@MemoryId String memoryId, String userInput);
}
