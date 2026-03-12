package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SkillWorkflowRunner;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepDef;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepDefLoader;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepProcessorRegistry;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgentInstanceRegistry;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgentRegistry;

import java.util.List;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 报表查询 skill 的 agentic 配置：注册 4 步 SubAgent 类与 4 个 StepProcessor（前/后处理），并暴露 ReportQuerySkillHandler。
 */
@Configuration
public class ReportAgenticConfig {

    private final SubAgentRegistry subAgentRegistry;
    private final StepProcessorRegistry stepProcessorRegistry;
    private final SemanticParseStepProcessor semanticParseStepProcessor;
    private final IntentExtractStepProcessor intentExtractStepProcessor;
    private final AlignStepProcessor alignStepProcessor;
    private final ReportParseStepProcessor reportParseStepProcessor;

    public ReportAgenticConfig(
            SubAgentRegistry subAgentRegistry,
            StepProcessorRegistry stepProcessorRegistry,
            SemanticParseStepProcessor semanticParseStepProcessor,
            IntentExtractStepProcessor intentExtractStepProcessor,
            AlignStepProcessor alignStepProcessor,
            ReportParseStepProcessor reportParseStepProcessor) {
        this.subAgentRegistry = subAgentRegistry;
        this.stepProcessorRegistry = stepProcessorRegistry;
        this.semanticParseStepProcessor = semanticParseStepProcessor;
        this.intentExtractStepProcessor = intentExtractStepProcessor;
        this.alignStepProcessor = alignStepProcessor;
        this.reportParseStepProcessor = reportParseStepProcessor;
    }

    @PostConstruct
    public void init() {
        subAgentRegistry
                .register(Agentic311Constants.Report.AGENT_SEMANTIC_PARSE, SemanticParseAgent.class)
                .register(Agentic311Constants.Report.AGENT_INTENT_EXTRACT, IntentExtractAgent.class)
                .register(Agentic311Constants.Report.AGENT_ALIGN, AlignAgent.class)
                .register(Agentic311Constants.Report.AGENT_REPORT_PARSE, ReportParseAgent.class);
        stepProcessorRegistry
                .register(Agentic311Constants.Report.PROCESSOR_SEMANTIC, semanticParseStepProcessor)
                .register(Agentic311Constants.Report.PROCESSOR_INTENT, intentExtractStepProcessor)
                .register(Agentic311Constants.Report.PROCESSOR_ALIGN, alignStepProcessor)
                .register(Agentic311Constants.Report.PROCESSOR_REPORT_PARSE, reportParseStepProcessor);
    }

    @Bean
    public SemanticParseAgent semanticParseAgent(ChatModel chatModel) {
        return AiServices.builder(SemanticParseAgent.class).chatModel(chatModel).build();
    }

    @Bean
    public IntentExtractAgent intentExtractAgent(ChatModel chatModel) {
        return AiServices.builder(IntentExtractAgent.class).chatModel(chatModel).build();
    }

    @Bean
    public AlignAgent alignAgent(ChatModel chatModel) {
        return AiServices.builder(AlignAgent.class).chatModel(chatModel).build();
    }

    @Bean
    public ReportParseAgent reportParseAgent(ChatModel chatModel) {
        return AiServices.builder(ReportParseAgent.class).chatModel(chatModel).build();
    }

    /** 供 catchAgentError 时在 agentAction 内调用实例并捕获异常。 */
    @Bean
    public SubAgentInstanceRegistry subAgentInstanceRegistry(
            SemanticParseAgent semanticParseAgent,
            IntentExtractAgent intentExtractAgent,
            AlignAgent alignAgent,
            ReportParseAgent reportParseAgent) {
        return new SubAgentInstanceRegistry()
                .register(Agentic311Constants.Report.AGENT_SEMANTIC_PARSE, semanticParseAgent)
                .register(Agentic311Constants.Report.AGENT_INTENT_EXTRACT, intentExtractAgent)
                .register(Agentic311Constants.Report.AGENT_ALIGN, alignAgent)
                .register(Agentic311Constants.Report.AGENT_REPORT_PARSE, reportParseAgent);
    }

    @Bean
    public ReportQuerySkillHandler reportQuerySkillHandler(SkillWorkflowRunner skillWorkflowRunner, StepDefLoader stepDefLoader) {
        List<StepDef> steps = stepDefLoader.load(Agentic311Constants.SkillHandlers.REPORT_QUERY);
        return new ReportQuerySkillHandler(skillWorkflowRunner, steps);
    }
}
