package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import dev.langchain4j.agentic.scope.AgenticScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 示例：步骤前/后处理。前处理从 scope 读上一步结果并写入 currentStepInput（此处仅透传，实际可查库、调接口后拼装）；
 * 后处理读本步结果可解析/落库后写回（此处仅打日志并透传）。
 */
@Component
public class DemoStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(DemoStepProcessor.class);

    @Override
    public void beforeStep(AgenticScope scope, String stepId, String previousOutputKey) {
        String prev = String.valueOf(scope.readState(previousOutputKey, ""));
        log.debug("beforeStep stepId={} prevKey={} len={}", stepId, previousOutputKey, prev.length());
        scope.writeState(SkillWorkflowRunner.CURRENT_STEP_INPUT, prev);
    }

    @Override
    public void afterStep(AgenticScope scope, String stepId, String stepResultKey) {
        String result = String.valueOf(scope.readState(stepResultKey, ""));
        log.debug("afterStep stepId={} resultKey={} len={}", stepId, stepResultKey, result.length());
        scope.writeState(stepResultKey, result);
    }
}
