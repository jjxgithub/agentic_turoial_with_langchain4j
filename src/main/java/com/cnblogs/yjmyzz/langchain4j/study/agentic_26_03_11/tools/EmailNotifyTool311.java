package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * 邮件通知工具（agentic_26_03_11 专用）：生成邮件内容或模拟发送，不执行真实发信。
 */
@Component("emailNotifyTool311")
public class EmailNotifyTool311 {

    @Tool("根据收件人邮箱、主题和正文生成或发送邮件。不执行真实发信时仅返回将发送的内容摘要。")
    public String generateOrSendEmail(
            @P("收件人邮箱") String toEmail,
            @P("邮件主题") String subject,
            @P("正文内容") String body) {
        if (toEmail == null || toEmail.isBlank()) {
            return "未提供收件人邮箱，未发送。";
        }
        String sub = subject != null ? subject : "(无主题)";
        String b = body != null ? body : "";
        return String.format("已生成邮件：收件人=%s，主题=%s，正文长度=%d 字符。（未执行真实发信）", toEmail.trim(), sub, b.length());
    }
}
