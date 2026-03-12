package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

/**
 * 报表四步的 prompt 模板，占位符为 ${name}，由各 StepProcessor 的 beforeStep 填充（可从 DB/接口获取）。
 */
public final class ReportPromptTemplates {

    private ReportPromptTemplates() {}

    /** 数据助理-语义解析模块：占位符 ${moduleInfos}、${link}、${module_code_1} 等为示例，实际由 moduleInfos 与用户输入组装。 */
    public static final String SEMANTIC_PARSE = """
        数据助理-语义解析模块 prompt
        ## 任务背景
        您需要根据提供的模块名称和对应的模块号，解析用户输入的查询语句以匹配相应的模块，并确定筛选条件之间的逻辑关系（"and" 或 "or"）。请注意，同一字段内的多个"或"条件应被视为该字段内部的组合，而不改变整体逻辑关系。

        ---

        ## 模块信息
        ${moduleInfos}

        ---

        ## 任务指令
        根据用户提供的查询语句，识别出相关的模块编号，并分析其中涉及的逻辑关系（"and" 或 "or"）。

        ### 返回格式要求
        ```json
        {
            "link": "${link}",
            "moduleCodeList": ["${module_code_1}", "${module_code_2}"]
        }
        ```

        ### 处理规则
        1. 当不同字段条件间用"或"连接时，`link` 值设为 `"or"`。
        2. 其他情况下（包括同一字段内多个"或"条件），`link` 值设为 `"and"`。
        3. 确保模块编码与系统定义的模块名称准确匹配。

        ---

        ## 注意事项
        1. **模块匹配**：确保用户查询中提到的模块名称与给定的模块信息表中的名称完全一致。
        2. **逻辑关系判断**：仔细区分不同字段间的逻辑关系（"and" 或 "or"）。同一字段内的多个"或"条件不应影响整体逻辑关系。
        3. **输出格式**：严格按照指定的JSON格式返回结果，仅输出 JSON，不要 Markdown 包裹或解释。

        ---

        ## 用户输入
        %s
        """;

    /** 数据助理-结构与意图提取：占位符在 beforeStep 中由上一步结果与用户输入组装。 */
    public static final String INTENT_EXTRACT = """
        数据助理-结构与意图提取 prompt

        # Role: 高级数据语义分析引擎 (Data Semantic Parser - Lite Execution)

        # Context
        你是一个 RAG 系统的核心解析层。你的任务是将自然语言查询抽象为"无字段名依赖"的逻辑 JSON。
        **架构策略**：后端仅支持原子查询，复杂的分析、对比和总结由 LLM 在获取数据后二次执行。

        # Task
        根据用户的 input 解析并提取意图、原子聚合、分组、排序及过滤条件。

        ## 关键转换逻辑
        1. **意图严格区分**: AGGREGATE(聚合/计算) 与 SELECTION(明细/清单)。
        2. **存在性转统计**: "有没有/是否存在" -> AGGREGATE + COUNT。
        3. **隐式汇总**: 最值/Top N 需对数值字段 SUM 或 COUNT，并按目标实体 groupBy。
        4. **维度提取**: "各"、"每个"、"按照...维度" -> 放入 groupBy。
        5. **时间缺省**: 未指定时间业务词时映射为"创建时间"。
        6. **身份识别**: "我"、"我的" -> 映射"创建人"或用户指定字段。

        ## 严格输出规范
        1. **JSON Only**: 严禁 Markdown 代码块或解释，仅输出 JSON。
        2. **占位符**: 业务实体映射为 field_1, field_2...
        3. **意图**: 仅允许 AGGREGATE 或 SELECTION。

        ## Output Schema
        {"queryType":"AGGREGATE|SELECTION","aggregations":[{"func":"SUM|COUNT","field":"field_n"}],"groupBy":[],"sort":[],"page":null|{"from":0,"size":10},"filters":[],"entitiesMapping":{"field_n":"用户原始词汇"}}

        ---

        ## 用户输入（含上一步语义解析结果若需要）
        %s
        """;

    /** 业务语义对齐专家：占位符 ${numberFieldNames}、${dateFieldNames}、${fieldNames} 由 beforeStep 从字段池填充。 */
    public static final String ALIGN = """
        业务语义对齐专家 prompt

        # Role: 业务语义对齐专家 (Business Semantic Aligner)

        # Context
        你处于 RAG 系统的核心环节。上一步已解析出用户的查询意图（Intent JSON）。
        现在你需要从可选的标准字段池中，为占位符（field_n）匹配最精准的业务字段名称。

        # Task
        1. 语义对齐：将 entitiesMapping 中的原始词汇映射到下方的"候选字段池"。
        2. 主体优先：当原始词命中多个候选时，优先匹配核心主体字段（短词）。
        3. 逻辑与类型校验：金额/数值->aggregations；日期/时间->filters WITHIN；基础/枚举->groupBy 或 ==。
        4. 输出逻辑 JSON：将原 JSON 中所有 field_n 替换为匹配到的标准业务名称。

        # 候选字段池
        ### 金额与指标模块 (数值型)
        ${numberFieldNames}

        ### 时间与维度模块 (日期型)
        ${dateFieldNames}

        ### 其他字段
        ${fieldNames}

        # Output Rules
        - JSON Only，严禁 Markdown 或解释。
        - includeMyself: 仅当用户输入显式包含"我"、"我的"、"本人"等时为 true，否则 false。
        - 相同 field_n 必须替换为同一标准名称。
        - 无匹配时替换为 "未找到对应字段"。

        ---

        ## 上一步意图 JSON（Input Intent）
        %s
        """;

    /** 报表意图解析专家：占位符 ${currentTime}、${myUserName}、${fieldNames} 等由 beforeStep 从元数据/DB 填充。 */
    public static final String REPORT_PARSE = """
        报表意图解析专家 prompt

        ## Role
        你是一个精准的报表意图解析专家。将用户输入的文本解析为预定义报表系统的 JSON 查询参数。

        ## Input Context
        - 当前系统时间：${currentTime}
        - 我的身份：${myUserName}
        - 可用元数据：
            - 字段池 (fieldNames): ${fieldNames}
            - 计数字段池 (countFieldNames): ${countFieldNames}
            - 求和字段池 (sumFieldNames): ${sumFieldNames}
            - 时间字段池 (dateFieldNames): ${dateFieldNames}
            - 过滤条件字段池 (filterFields): ${filterFields}
            - 分组字段池 (groupFieldNames): ${groupFieldNames}
            - 排序字段池 (orderByFieldNames): ${orderByFieldNames}
            - 筛选映射 (field2SelectionNames): ${field2SelectionNames}

        ## Workflow & Rules
        1. fieldList: 仅从 fieldNames 匹配；当 fieldNames 非空时自动包含：客户、所属人。
        2. sumList/countList: 仅从 sumFieldNames/countFieldNames 匹配。
        3. amountList: 若 sumList 含金额/收入/合同额/费用等，同步填入 amountList。
        4. filterList: 从 filterFields 匹配；仅当用户输入含"我"、"我的"、"本人"时才添加创建人过滤。
        5. 时间规范：相对时间转为 "yyyy-MM-dd HH:mm:ss" 绝对区间。
        6. groupList: 优先 groupFieldNames；无显式分组时兜底：创建人 -> 创建时间(yyyy-MM) -> 所属部门。
        7. order: 严格匹配 orderByFieldNames，desc 仅 "desc" 或 "asc"。
        8. 所有字段名必须严格存在于候选列表，严禁幻觉。

        ## Output Format（仅输出 JSON，禁止解释）
        {"fieldList":[],"sumList":[],"countList":[],"groupList":[{"field":"","format":""}],"order":{"field":"","desc":""},"filterList":[],"usdExchangeRate":"","amountList":[]}

        ---

        ## 上一步对齐结果（Intent + 对齐后的 JSON）
        %s
        """;
}
