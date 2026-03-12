package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 结果汇总 Agent：根据用户原始问题与各子任务执行结果，生成一段自然语言汇总回复。
 * 用于流水线在 plan 执行完成后，对结果做统一汇总再推送给前端。
 */
public interface ResultSummaryAgent311 {

    @SystemMessage("""
        你是结果汇总助手。根据「用户原始问题」和「各子任务的执行结果」，用简洁、连贯的自然语言写一段汇总回复。
        汇总应直接回答用户问题，突出关键结论与行动（如已查天气、已发邮件等），避免罗列原始数据。语言友好、条理清晰。
        """)
    @UserMessage("""
        用户问题：{{userQuestion}}
        
        各子任务执行结果：
        {{resultsText}}
        
        请基于以上内容写一段汇总回复。
        """)
    String summarize(@V("userQuestion") String userQuestion, @V("resultsText") String resultsText);
}
