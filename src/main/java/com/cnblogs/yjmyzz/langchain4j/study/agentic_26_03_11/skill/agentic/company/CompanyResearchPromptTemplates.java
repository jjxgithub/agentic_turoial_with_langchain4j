package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

/**
 * 公司调研报告 Prompt 模板。占位符由第 2 步 StepProcessor 从第 1 步结果中填充。
 * 可替换为从 DB/配置中心读取，实现按业务定制的调研框架。
 */
public final class CompanyResearchPromptTemplates {

    private CompanyResearchPromptTemplates() {}

    /** 占位符：公司名称、官网 URL、第 1 步原始结果（若未解析出 JSON 则用整段文本）。 */
    public static final String COMPANY_RESEARCH_REPORT = """
        ## 报告要求
        请根据下方「信息来源」撰写一份**公司调研报告**，结构清晰、分点陈述，面向内部汇报或投资参考。

        ### 必须包含的章节（可酌情合并，但需覆盖）
        1. **公司概况**：公司名称、官网、简要定位（一句话）。
        2. **主营业务与产品/服务**：主要业务线、核心产品或服务、目标客户。
        3. **技术/能力亮点**：技术优势、专利、研发或运营特色（如有）。
        4. **市场与竞争**：所处行业、市场地位或典型客户（可基于公开信息推断）。
        5. **风险与挑战**：可能面临的政策、市场、竞争等风险（可简要提及）。
        6. **总结与建议**：2～3 条结论或后续可跟进方向。

        ### 写作要求
        - 语言简洁、客观，避免主观夸大。
        - 若官网未找到或信息不足，在对应章节注明「信息有限」并基于公司名称做合理推断与提示。
        - 输出为 Markdown 格式，便于阅读与存档。

        ---

        ## 信息来源（第 1 步：官网查找结果）
        ${step1Result}

        ---
        请根据以上信息生成公司调研报告。
        """;
}
