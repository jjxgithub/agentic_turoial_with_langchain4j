package com.cnblogs.yjmyzz.langchain4j.study.agentic_26_03_11;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 通用依赖成功判断 Agent：根据「任务描述 + 任务执行结果」判断该任务是否成功完成。
 * 不穷举关键词，由 LLM 理解语义；结果写入 scope 的 {taskId}_judge，供后续任务条件使用。
 */
public interface DependencySuccessJudge {

    String SUCCESS = "SUCCESS";
    String FAILURE = "FAILURE";

    @Agent("根据任务描述与执行结果判断该任务是否成功完成，仅输出 SUCCESS 或 FAILURE")
    @SystemMessage("""
        你根据「任务描述」与「任务执行结果」判断该任务是否成功完成。
        若结果表示任务达成目标、得到有效输出，则输出 SUCCESS。
        若结果表示无法完成、失败、不可用、仅给出替代建议而无实际结果等，则输出 FAILURE。
        仅输出一个词：SUCCESS 或 FAILURE，不要其他内容。
        """)
    @UserMessage("""
        任务描述：{{currentJudgeTaskQuestion}}
        执行结果：{{currentJudgeTaskResult}}
        """)
    String judge(
            @V("currentJudgeTaskId") String taskId,
            @V("currentJudgeTaskQuestion") String taskQuestion,
            @V("currentJudgeTaskResult") String taskResult
    );
}
