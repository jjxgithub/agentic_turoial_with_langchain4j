package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.SkillWorkflowRunner;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.StepDefLoader;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.tools.CompanyOfficialWebsiteSearchTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 公司相关 Skill 的 Bean 定义：官网查找、网站分析共用 Agent/Processor，拆成两个 Handler 便于匹配与扩展。
 */
@Configuration
public class CompanyAnalysisBeansConfig {

    @Bean
    public FindOfficialWebsiteAgent findOfficialWebsiteAgent(ChatModel chatModel, CompanyOfficialWebsiteSearchTool companyOfficialWebsiteSearchTool) {
        return AiServices.builder(FindOfficialWebsiteAgent.class)
                .chatModel(chatModel)
                .tools(companyOfficialWebsiteSearchTool)
                .build();
    }

    @Bean
    public CompanyAnalysisReportAgent companyAnalysisReportAgent(ChatModel chatModel) {
        return AiServices.builder(CompanyAnalysisReportAgent.class).chatModel(chatModel).build();
    }

    @Bean
    public CompanyFindWebsiteSkillHandler companyFindWebsiteSkillHandler(SkillWorkflowRunner skillWorkflowRunner, StepDefLoader stepDefLoader) {
        var steps = stepDefLoader.load(Agentic311Constants.SkillHandlers.COMPANY_FIND_WEBSITE);
        return new CompanyFindWebsiteSkillHandler(skillWorkflowRunner, steps);
    }

    @Bean
    public CompanyWebsiteAnalysisSkillHandler companyWebsiteAnalysisSkillHandler(SkillWorkflowRunner skillWorkflowRunner, StepDefLoader stepDefLoader) {
        var steps = stepDefLoader.load(Agentic311Constants.SkillHandlers.COMPANY_WEBSITE_ANALYSIS);
        return new CompanyWebsiteAnalysisSkillHandler(skillWorkflowRunner, steps);
    }
}
