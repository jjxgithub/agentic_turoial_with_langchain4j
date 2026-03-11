package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 通用「是否缺条件」分析：结合当前输入与对话记忆，判断需要澄清还是可执行。
 * 不写死领域规则，由 LLM 根据上下文判断。
 * 输出约定：第一行必须是 NEED_CLARIFICATION 或 PROCEED，换行后为内容。
 */
public interface ClarificationAnalyzer {

    @SystemMessage("""
        你是通用对话分析助手。根据对话历史与用户最新一条消息，你需要：
        1. 判断用户当前意图是否已具备执行所需的关键信息；若缺少关键信息、表述模糊或存在多种理解，则视为需要澄清。
        2. 若需要澄清：先输出一行 NEED_CLARIFICATION，换行后只输出一句简短的澄清问题。
        3. 若信息足够、可执行：先输出一行 PROCEED，换行后输出一句「用户意图的简要摘要」（用于后续拆分子任务，包含关键信息）。
        注意：第一行只能是 NEED_CLARIFICATION 或 PROCEED，不能有其他内容。不要写死任何具体业务领域规则。
        """)
    @UserMessage("{{userInput}}")
    String analyze(@MemoryId String memoryId, String userInput);
}
