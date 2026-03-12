package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepProcessorRegistry;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgentInstanceRegistry;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SubAgentRegistry;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.ToolRegistry;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools.CompanyOfficialWebsiteSearchTool;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * 公司官网与简析 Skill 的注册配置：在 init() 中注册 SubAgent、StepProcessor、Tool 及实例到全局 Registry。
 * Bean 定义在 {@link CompanyAnalysisBeansConfig} 中，避免本类自循环。
 */
@Configuration
public class CompanyAnalysisAgenticConfig {

    private final SubAgentRegistry subAgentRegistry;
    private final StepProcessorRegistry stepProcessorRegistry;
    private final ToolRegistry toolRegistry;
    private final FindOfficialWebsiteStepProcessor findOfficialWebsiteStepProcessor;
    private final CompanyAnalysisReportStepProcessor companyAnalysisReportStepProcessor;
    private final CompanyOfficialWebsiteSearchTool companyOfficialWebsiteSearchTool;
    private final SubAgentInstanceRegistry subAgentInstanceRegistry;
    private final ApplicationContext applicationContext;

    public CompanyAnalysisAgenticConfig(
            SubAgentRegistry subAgentRegistry,
            StepProcessorRegistry stepProcessorRegistry,
            ToolRegistry toolRegistry,
            SubAgentInstanceRegistry subAgentInstanceRegistry,
            FindOfficialWebsiteStepProcessor findOfficialWebsiteStepProcessor,
            CompanyAnalysisReportStepProcessor companyAnalysisReportStepProcessor,
            CompanyOfficialWebsiteSearchTool companyOfficialWebsiteSearchTool,
            ApplicationContext applicationContext) {
        this.subAgentRegistry = subAgentRegistry;
        this.stepProcessorRegistry = stepProcessorRegistry;
        this.toolRegistry = toolRegistry;
        this.subAgentInstanceRegistry = subAgentInstanceRegistry;
        this.findOfficialWebsiteStepProcessor = findOfficialWebsiteStepProcessor;
        this.companyAnalysisReportStepProcessor = companyAnalysisReportStepProcessor;
        this.companyOfficialWebsiteSearchTool = companyOfficialWebsiteSearchTool;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        subAgentRegistry
                .register(Agentic311Constants.CompanyAnalysis.AGENT_FIND_OFFICIAL_WEBSITE, FindOfficialWebsiteAgent.class)
                .register(Agentic311Constants.CompanyAnalysis.AGENT_ANALYSIS_REPORT, CompanyAnalysisReportAgent.class);
        stepProcessorRegistry
                .register(Agentic311Constants.CompanyAnalysis.PROCESSOR_FIND_WEBSITE, findOfficialWebsiteStepProcessor)
                .register(Agentic311Constants.CompanyAnalysis.PROCESSOR_ANALYSIS_REPORT, companyAnalysisReportStepProcessor);
        toolRegistry.register(Agentic311Constants.ToolIds.COMPANY_OFFICIAL_WEBSITE_SEARCH, companyOfficialWebsiteSearchTool);
        FindOfficialWebsiteAgent findAgent = applicationContext.getBean(FindOfficialWebsiteAgent.class);
        CompanyAnalysisReportAgent reportAgent = applicationContext.getBean(CompanyAnalysisReportAgent.class);
        subAgentInstanceRegistry
                .register(Agentic311Constants.CompanyAnalysis.AGENT_FIND_OFFICIAL_WEBSITE, findAgent)
                .register(Agentic311Constants.CompanyAnalysis.AGENT_ANALYSIS_REPORT, reportAgent);
    }
}
