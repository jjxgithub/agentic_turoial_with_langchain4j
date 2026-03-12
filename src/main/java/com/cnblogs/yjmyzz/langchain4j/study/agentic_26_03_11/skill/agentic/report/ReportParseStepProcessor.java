package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SkillWorkflowRunner;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepProcessor;
import dev.langchain4j.agentic.scope.AgenticScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 报表第 4 步「报表解析」的前/后处理：前处理从系统/元数据组装当前时间、用户身份、字段池等（可查库/接口）；后处理可校验/落库或触发报表查询。
 */
@Component
public class ReportParseStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReportParseStepProcessor.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void beforeStep(AgenticScope scope, String stepId, String previousOutputKey) {
        String alignJson = String.valueOf(scope.readState(previousOutputKey, "")).trim();

        // TODO: 从系统上下文或数据库获取：当前时间、当前用户、可用元数据（字段池、计数字段池、求和字段池等）
        // String currentTime = systemContext.getCurrentTime();
        // String myUserName = securityContext.getCurrentUserName();
        // ReportMetadata meta = reportMetadataService.getByModule(moduleCode);
        String currentTime = LocalDateTime.now().format(FMT);
        String myUserName = "当前用户";
        String fieldNames = "客户,所属人,所属部门,创建时间,成交额,订单数";
        String countFieldNames = "订单数,客户数";
        String sumFieldNames = "成交额,费用,收入,合同额";
        String dateFieldNames = "创建时间,更新时间,成交时间,下单日期";
        String filterFields = "创建人,所属人,地区,客户,状态";
        String groupFieldNames = "创建人,所属部门,创建时间,客户";
        String orderByFieldNames = "成交额,创建时间,订单数";
        String field2SelectionNames = "{}";

        String prompt = ReportPromptTemplates.REPORT_PARSE
                .replace("${currentTime}", currentTime)
                .replace("${myUserName}", myUserName)
                .replace("${fieldNames}", fieldNames)
                .replace("${countFieldNames}", countFieldNames)
                .replace("${sumFieldNames}", sumFieldNames)
                .replace("${dateFieldNames}", dateFieldNames)
                .replace("${filterFields}", filterFields)
                .replace("${groupFieldNames}", groupFieldNames)
                .replace("${orderByFieldNames}", orderByFieldNames)
                .replace("${field2SelectionNames}", field2SelectionNames);
        prompt = String.format(prompt, alignJson);
        scope.writeState(SkillWorkflowRunner.CURRENT_STEP_INPUT, prompt);
    }

    @Override
    public void afterStep(AgenticScope scope, String stepId, String stepResultKey) {
        String raw = String.valueOf(scope.readState(stepResultKey, ""));
        // TODO: 解析最终报表 JSON，校验字段均在候选池内；可调用报表查询接口/数据服务执行查询并写入 scope 或返回
        // ReportQueryParams params = objectMapper.readValue(raw, ReportQueryParams.class);
        // Object queryResult = reportQueryService.execute(params);
        // scope.writeState(stepResultKey, objectMapper.writeValueAsString(queryResult));
        log.debug("report_parse afterStep stepId={} resultLen={}", stepId, raw.length());
        scope.writeState(stepResultKey, raw);
    }
}
