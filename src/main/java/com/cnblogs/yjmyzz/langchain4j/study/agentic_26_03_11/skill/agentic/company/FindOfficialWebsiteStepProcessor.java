package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepProcessor;
import dev.langchain4j.agentic.scope.AgenticScope;
import org.springframework.stereotype.Component;

/**
 * 公司官网与简析 — 第 1 步前处理：将用户输入（公司名称）写入 currentStepInput，供 Agent 与 Tool 使用。
 */
@Component
public class FindOfficialWebsiteStepProcessor implements StepProcessor {

    @Override
    public void beforeStep(AgenticScope scope, String stepId, String previousOutputKey) {
        String input = String.valueOf(scope.readState(previousOutputKey, "")).trim();
        if (input.isEmpty()) {
            input = "请从上下文识别要查询的公司名称；若无法识别则回复：未提供公司名称。";
        }
        scope.writeState(Agentic311Constants.ScopeKeys.CURRENT_STEP_INPUT, input);
    }
}
