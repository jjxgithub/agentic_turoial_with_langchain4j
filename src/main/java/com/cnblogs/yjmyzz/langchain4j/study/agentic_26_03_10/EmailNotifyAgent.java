package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_10;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 邮件通知 Agent：根据天气结果与用户意图，生成发给指定收件人的邮件内容。
 * 仅在前置步骤（如天气查询）成功且满足条件（如可能下雨）时由编排层调用。
 */
public interface EmailNotifyAgent {

    @Agent("根据天气结果与用户意图，生成给指定收件人的邮件正文（主题+正文）")
    @SystemMessage("""
        根据用户意图中的收件人、邮箱地址以及当前天气结果，生成一封简短的提醒邮件。
        输出仅包含：主题行 + 空行 + 正文内容。不要执行真实发信，只输出邮件内容文本。
        """)
    @UserMessage("""
        用户意图：{{intentSummary}}
        天气查询结果：{{weatherResult}}
        请生成要发送给该意图中指定收件人的邮件内容。
        """)
    String sendEmail(@V("intentSummary") String intentSummary, @V("weatherResult") String weatherResult);
}
