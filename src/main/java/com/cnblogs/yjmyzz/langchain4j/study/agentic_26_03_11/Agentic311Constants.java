package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

/**
 * agentic_26_03_11 包内统一常量，避免魔法字符串分散在各 Config/Handler 中。
 */
public final class Agentic311Constants {

    private Agentic311Constants() {}

    /** Skill 工作流 scope 中的 key：入口输入、每步输入、每步结果 key 约定、错误 payload 模板。 */
    public static final class ScopeKeys {
        private ScopeKeys() {}
        public static final String SKILL_INPUT = "skill_input";
        public static final String CURRENT_STEP_INPUT = "currentStepInput";
        public static final String STEP_RESULT_PREFIX = "step_";
        public static final String STEP_RESULT_SUFFIX = "_result";
        public static final String ERROR_PAYLOAD_TEMPLATE = "{\"error\":true,\"stepId\":\"%s\",\"message\":\"%s\"}";
    }

    /** 重试默认：StepDef.agentRetryCount、ToolMeta.retryCount 未指定时均为 0（不重试）。 */
    public static final class Retry {
        private Retry() {}
        public static final int DEFAULT_AGENT_RETRY_COUNT = 0;
    }

    /** StepDef.toolIds 与 ToolRegistry 注册 id。 */
    public static final class ToolIds {
        private ToolIds() {}
        public static final String RELATIVE_TIME_RESOLVER = "relativeTimeResolver";
    }

    /** Demo Skill 的 StepProcessor id。 */
    public static final class Demo {
        private Demo() {}
        public static final String PROCESSOR_ID = "demo";
    }

    /** 报表查询 Skill：4 步 Agent id 与对应 StepProcessor id。 */
    public static final class Report {
        private Report() {}
        public static final String AGENT_SEMANTIC_PARSE = "semantic_parse";
        public static final String AGENT_INTENT_EXTRACT = "intent_extract";
        public static final String AGENT_ALIGN = "align";
        public static final String AGENT_REPORT_PARSE = "report_parse";
        public static final String PROCESSOR_SEMANTIC = "report_semantic";
        public static final String PROCESSOR_INTENT = "report_intent";
        public static final String PROCESSOR_ALIGN = "report_align";
        public static final String PROCESSOR_REPORT_PARSE = "report_report_parse";
    }

    /** SkillHandlerRegistry 注册用的 handlerId，与 skills/*.md 中 handlerId 一致。 */
    public static final class SkillHandlers {
        private SkillHandlers() {}
        public static final String GREETING = "greeting";
        public static final String FAREWELL = "farewell";
        public static final String REPORT_QUERY = "report_query";
    }
}
