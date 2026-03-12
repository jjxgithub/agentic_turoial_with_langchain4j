package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepProcessor;
import dev.langchain4j.agentic.scope.AgenticScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 报表第 1 步「语义解析」的前/后处理：前处理组装模块信息与用户输入成完整 prompt；后处理可校验/落库。
 */
@Component
public class SemanticParseStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(SemanticParseStepProcessor.class);

    @Override
    public void beforeStep(AgenticScope scope, String stepId, String previousOutputKey) {
        String userInput = String.valueOf(scope.readState(previousOutputKey, "")).trim();
        if (userInput.isEmpty()) userInput = "（无用户输入）";

        // TODO: 从数据库或配置接口加载模块信息，例如：
        // List<ModuleInfo> list = moduleInfoRepository.findAll();
        // String moduleInfos = list.stream().map(m -> m.getCode() + ":" + m.getName()).collect(Collectors.joining("\n"));
        String moduleInfos = """
            NewSC017:商机
            NewBF001:客户
            NewSC002:订单
            """;

        String prompt = ReportPromptTemplates.SEMANTIC_PARSE
                .replace("${moduleInfos}", moduleInfos)
                .replace("${link}", "and")
                .replace("${module_code_1}", "")
                .replace("${module_code_2}", "");
        prompt = String.format(prompt, userInput);
        scope.writeState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, prompt);
    }

    @Override
    public void afterStep(AgenticScope scope, String stepId, String stepResultKey) {
        String raw = String.valueOf(scope.readState(stepResultKey, ""));
        // TODO: 可选 - 解析 JSON 校验 link/moduleCodeList，或调用中间层接口落库/打标
        // SemanticParseResult result = objectMapper.readValue(raw, SemanticParseResult.class);
        // semanticParseResultRepository.save(result);
        log.debug("semantic_parse afterStep stepId={} resultLen={}", stepId, raw.length());
        scope.writeState(stepResultKey, raw);
    }
}
