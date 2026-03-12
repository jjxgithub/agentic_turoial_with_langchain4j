package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic;

import java.io.Serial;

/**
 * Step 校验失败时抛出，表示某步引用的 agentId / preProcessorId / postProcessorId 未在对应注册表中注册。
 * 用于构建前快速失败，避免静默降级。
 */
public class StepValidationException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = -3587964448915265548L;
    private final String stepId;
    private final String detail;

    public StepValidationException(String stepId, String detail) {
        super("Step validation failed: stepId=" + (stepId != null ? stepId : "?") + ", " + (detail != null ? detail : ""));
        this.stepId = stepId;
        this.detail = detail;
    }

    public String getStepId() {
        return stepId;
    }

    public String getDetail() {
        return detail;
    }
}
