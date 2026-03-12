package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.Agentic311Constants;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company.CompanyFindWebsiteSkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.company.CompanyWebsiteAnalysisSkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.agentic.report.ReportQuerySkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.DetectLanguageStep;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.FarewellReplyStep;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.FarewellSkillHandler;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.GenerateGreetingStep;
import com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill.demo.GreetingSkillHandler;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.List;

/**
 * Skill 发现与 Demo 配置：Handler 按 handlerId 注册，Skill 从 SKILL.md 加载后与 Handler 绑定。
 */
@Configuration
public class SkillDemoConfig {

    @Bean
    public DetectLanguageStep detectLanguageStep(ChatModel chatModel) {
        return AiServices.builder(DetectLanguageStep.class).chatModel(chatModel).build();
    }

    @Bean
    public GenerateGreetingStep generateGreetingStep(ChatModel chatModel) {
        return AiServices.builder(GenerateGreetingStep.class).chatModel(chatModel).build();
    }

    @Bean
    public FarewellReplyStep farewellReplyStep(ChatModel chatModel) {
        return AiServices.builder(FarewellReplyStep.class).chatModel(chatModel).build();
    }

    /** handlerId → SkillHandler，供 SKILL.md 中的 handlerId 解析。@Lazy 避免与 CompanyAnalysisAgenticConfig 循环依赖。 */
    @Bean
    public SkillHandlerRegistry skillHandlerRegistry(
            GreetingSkillHandler greetingSkillHandler,
            FarewellSkillHandler farewellSkillHandler,
            ReportQuerySkillHandler reportQuerySkillHandler,
            @Lazy CompanyFindWebsiteSkillHandler companyFindWebsiteSkillHandler,
            @Lazy CompanyWebsiteAnalysisSkillHandler companyWebsiteAnalysisSkillHandler) {
        return new SkillHandlerRegistry()
                .register(Agentic311Constants.SkillHandlers.GREETING, greetingSkillHandler)
                .register(Agentic311Constants.SkillHandlers.FAREWELL, farewellSkillHandler)
                .register(Agentic311Constants.SkillHandlers.REPORT_QUERY, reportQuerySkillHandler)
                .register(Agentic311Constants.SkillHandlers.COMPANY_FIND_WEBSITE, companyFindWebsiteSkillHandler)
                .register(Agentic311Constants.SkillHandlers.COMPANY_WEBSITE_ANALYSIS, companyWebsiteAnalysisSkillHandler);
    }

    /** LLM 技能路由：根据用户输入与技能描述选择最匹配的 skill id。 */
    @Bean
    public SkillRouter skillRouter(ChatModel chatModel) {
        return AiServices.builder(SkillRouter.class)
                .chatModel(chatModel)
                .build();
    }

    /** 使用 LLM 做 skill 匹配（行业通用做法）。 */
    @Bean
    public SkillMatcher skillMatcher(SkillRouter skillRouter) {
        return new LlmSkillMatcher(skillRouter);
    }

    /** 从 classpath:skills/*.md 加载 skill 定义，并绑定 Handler；匹配策略为 LLM 路由。 */
    @Bean
    public SkillRegistry skillRegistry(SkillHandlerRegistry skillHandlerRegistry, SkillMatcher skillMatcher) {
        List<Skill> fromMd = SkillMarkdownLoader.loadFromClasspath("classpath*:skills/*.md", skillHandlerRegistry);
        SkillRegistry registry = new SkillRegistry(skillMatcher);
        registry.registerAll(fromMd);
        return registry;
    }
}
