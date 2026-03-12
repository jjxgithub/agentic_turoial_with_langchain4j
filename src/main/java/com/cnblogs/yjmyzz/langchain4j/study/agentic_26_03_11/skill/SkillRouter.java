package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11.skill;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LLM 技能路由：根据用户输入和技能列表返回最匹配的技能 id，或无匹配时返回 "none"。
 * 用于行业常见的 LLM-based skill routing。
 */
public interface SkillRouter {

    @SystemMessage("""
        你是技能路由。根据用户输入和下面的技能列表，选择最匹配的一个技能。
        只输出该技能的 id（与列表中完全一致），若无任何技能匹配则只输出：none
        不要输出解释、标点或其它内容，仅一行：一个 id 或 none。""")
    @UserMessage("""
        技能列表（每项格式为 id / name / description）：
        {{skillsDescription}}

        用户输入：{{userInput}}""")
    String selectSkillId(String skillsDescription, String userInput);
}
