package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepProcessor;
import dev.langchain4j.agentic.scope.AgenticScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 报表第 3 步「对齐」的前/后处理：前处理从字段池组装候选列表（可查库/接口）；后处理可校验/落库。
 */
@Component
public class AlignStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(AlignStepProcessor.class);

    @Override
    public void beforeStep(AgenticScope scope, String stepId, String previousOutputKey) {
        String intentJson = String.valueOf(scope.readState(previousOutputKey, "")).trim();

        // TODO: 从数据库或元数据接口获取各模块的字段池，例如：
        // numberFieldNames = fieldMetadataService.getNumberFieldNames(moduleCode);
        // dateFieldNames = fieldMetadataService.getDateFieldNames(moduleCode);
        // fieldNames = fieldMetadataService.getOtherFieldNames(moduleCode);
        String numberFieldNames = "成交额, 订单金额, 费用, 收入, 合同额";
        String dateFieldNames = "创建时间, 更新时间, 成交时间, 下单日期";
        String fieldNames = "客户, 客户名称, 所属人, 所属部门, 地区, 租户名";

        String prompt = ReportPromptTemplates.ALIGN
                .replace("${numberFieldNames}", numberFieldNames)
                .replace("${dateFieldNames}", dateFieldNames)
                .replace("${fieldNames}", fieldNames);
        prompt = String.format(prompt, intentJson);
        scope.writeState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, prompt);
    }

    @Override
    public void afterStep(AgenticScope scope, String stepId, String stepResultKey) {
        String raw = String.valueOf(scope.readState(stepResultKey, ""));
        // TODO: 可选 - 解析对齐结果，校验 includeMyself/字段名是否在候选池，或调用对齐服务落库
        // AlignResult result = objectMapper.readValue(raw, AlignResult.class);
        // alignResultRepository.save(result);
        log.debug("align afterStep stepId={} resultLen={}", stepId, raw.length());
        scope.writeState(stepResultKey, raw);
    }
}
