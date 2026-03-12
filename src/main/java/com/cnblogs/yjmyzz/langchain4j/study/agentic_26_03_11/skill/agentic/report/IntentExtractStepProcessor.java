package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepProcessor;
import dev.langchain4j.agentic.scope.AgenticScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 报表第 2 步「意图提取」的前/后处理：前处理将上一步语义解析结果与用户输入组装成 prompt；后处理可校验/落库。
 */
@Component
public class IntentExtractStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(IntentExtractStepProcessor.class);

    @Override
    public void beforeStep(AgenticScope scope, String stepId, String previousOutputKey) {
        String prev = String.valueOf(scope.readState(previousOutputKey, "")).trim();
        // 首步为 skill_input（用户原问），否则为上一步的 JSON 结果；意图提取需要用户原问 + 可选语义解析结果
        String prompt = String.format(ReportPromptTemplates.INTENT_EXTRACT, prev);
        scope.writeState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, prompt);
    }

    @Override
    public void afterStep(AgenticScope scope, String stepId, String stepResultKey) {
        String raw = String.valueOf(scope.readState(stepResultKey, ""));
        // TODO: 可选 - 解析 JSON 校验 queryType/aggregations/filters 等，或调用意图服务落库
        // IntentExtractResult result = objectMapper.readValue(raw, IntentExtractResult.class);
        // intentExtractService.save(result);
        log.debug("intent_extract afterStep stepId={} resultLen={}", stepId, raw.length());
        scope.writeState(stepResultKey, raw);
    }
}
