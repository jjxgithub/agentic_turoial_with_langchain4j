# langchain4j-agentic 框架梳理

> 基于 [langchain4j-agentic](https://github.com/langchain4j/langchain4j/tree/main/langchain4j-agentic) 源码与 [agentic-tutorial](https://github.com/langchain4j/langchain4j-examples/tree/main/agentic-tutorial) 示例工程整理。

---

## 一、框架思想（概要）

langchain4j-agentic 是 LangChain4j 的**智能体编排层**，核心思想可以概括为：

1. **声明式 Agent 定义**：用接口 + `@Agent` / `@SequenceAgent` / `@ParallelAgent` 等注解描述“谁做什么、如何编排”，而不是手写调用链。
2. **统一编排抽象**：所有编排（顺序、并行、条件、循环、监督者）都抽象成 **Planner + AgenticScope**：Planner 决定“下一步调用谁”，Scope 在各 Agent 之间共享输入/输出状态。
3. **单 Agent = AiServices + 工具**：每个叶子 Agent 由 `AgentBuilder` 包装成基于 LangChain4j `AiServices` 的代理，可挂 Tool、RAG、Guardrail 等。
4. **可组合、可观测**：支持子 Agent 组合成复合 Agent、Human-in-the-loop、监听器（AgentListener）与监控（AgentMonitor）。

---

## 二、结构图（概要）

```mermaid
flowchart TB
    subgraph 入口["入口层"]
        AgenticServices["AgenticServices"]
    end

    subgraph 编排方式["编排方式"]
        Sequence["Sequence 顺序"]
        Parallel["Parallel 并行"]
        Conditional["Conditional 条件"]
        Loop["Loop 循环"]
        Supervisor["Supervisor 监督者"]
        Planner["Planner 规划器"]
    end

    subgraph 运行时["运行时核心"]
        PlannerCore["Planner (决定下一步)"]
        AgenticScope["AgenticScope (共享状态)"]
        AgentExecutor["AgentExecutor (执行单次调用)"]
    end

    subgraph 单Agent["单 Agent"]
        AgentBuilder["AgentBuilder"]
        AiServices["AiServices (LLM+Tools)"]
    end

    AgenticServices --> Sequence
    AgenticServices --> Parallel
    AgenticServices --> Conditional
    AgenticServices --> Loop
    AgenticServices --> Supervisor
    AgenticServices --> Planner

    Sequence --> PlannerCore
    Parallel --> PlannerCore
    Conditional --> PlannerCore
    Loop --> PlannerCore
    Supervisor --> PlannerCore

    PlannerCore --> AgenticScope
    PlannerCore --> AgentExecutor
    AgentExecutor --> AgentBuilder
    AgentBuilder --> AiServices
```

---

## 三、结构图（详细）

### 3.1 包与模块划分

```mermaid
flowchart LR
    subgraph agentic["dev.langchain4j.agentic"]
        A1[Agent.java\nUntypedAgent.java]
        A2[AgenticServices.java]
    end

    subgraph agent["agent"]
        B1[AgentBuilder]
        B2[AgentInvocationHandler]
        B3[UntypedAgentBuilder]
    end

    subgraph declarative["declarative"]
        C1[SequenceAgent\nParallelAgent\nConditionalAgent]
        C2[LoopAgent\nSupervisorAgent\nPlannerAgent]
        C3[ActivationCondition\nExitCondition\nTypedKey]
        C4[ChatModelSupplier\nContentRetrieverSupplier...]
    end

    subgraph scope["scope"]
        D1[AgenticScope]
        D2[DefaultAgenticScope]
        D3[ResultWithAgenticScope]
    end

    subgraph planner["planner"]
        E1[Planner]
        E2[AgentInstance\nAction]
        E3[PlanningContext\nInitPlanningContext]
    end

    subgraph workflow["workflow"]
        F1[SequentialAgentService\nParallelAgentService]
        F2[ConditionalAgentService\nLoopAgentService]
        F3[WorkflowAgentsBuilder\n*Planner]
    end

    subgraph supervisor["supervisor"]
        G1[SupervisorAgentService\nSupervisorAgentServiceImpl]
        G2[SupervisorPlanner\nSupervisorContextStrategy]
        G3[SupervisorResponseStrategy]
    end

    subgraph internal["internal"]
        H1[AgentExecutor\nAgentInvoker]
        H2[AgenticScopeOwner\nContext]
    end

    A2 --> B1
    A2 --> F1
    A2 --> G1
    B1 --> B2
    B2 --> H1
    F1 --> E1
    G1 --> G2
    E1 --> D1
    H1 --> D2
```

### 3.2 与示例工程的对应关系

| 示例目录 (agentic-tutorial) | 使用的框架能力 |
|-----------------------------|----------------|
| `_1_basic_agent` | `AgenticServices.agentBuilder(Class)`、`@Agent` |
| `_2_sequential_workflow` | `sequenceBuilder()`、`outputKey`、Scope 状态传递 |
| `_3_loop_workflow` | `loopBuilder()`、`ExitCondition`、循环直到条件满足 |
| `_4_parallel_workflow` | `parallelBuilder()`、多子 Agent 并行执行 |
| `_5_conditional_workflow` | `conditionalBuilder()`、`@ActivationCondition` 路由 |
| `_6_composed_workflow` | 顺序/并行/条件等组合成更大工作流 |
| `_7_supervisor_orchestration` | `supervisorBuilder()`、监督者动态选子 Agent |
| `_8_non_ai_agents` | 非 AI 的“Agent”（纯函数）参与编排 |
| `_9_human_in_the_loop` | `humanInTheLoopBuilder()` |
| `_a_react` | 单 Agent + Tools（ReAct 风格） |
| `_b_plan_and_execute` | `plannerBuilder()`、自定义 Planner |

---

## 四、核心类图

### 4.1 入口与 Builder

```mermaid
classDiagram
    class AgenticServices {
        <<static>>
        +agentBuilder(Class) AgentBuilder
        +agentBuilder() UntypedAgentBuilder
        +sequenceBuilder() SequentialAgentService
        +sequenceBuilder(Class) SequentialAgentService
        +parallelBuilder() ParallelAgentService
        +conditionalBuilder() ConditionalAgentService
        +loopBuilder() LoopAgentService
        +supervisorBuilder() SupervisorAgentService
        +supervisorBuilder(Class) SupervisorAgentService
        +plannerBuilder() PlannerBasedService
        +createAgenticSystem(Class) T
    }

    class AgentBuilder {
        -agentServiceClass Class
        -agenticMethod Method
        +chatModel(ChatModel) B
        +tools(Object...) B
        +outputKey(String) B
        +build() T
    }

    class UntypedAgentBuilder {
        +chatModel(ChatModel) B
        +build() UntypedAgent
    }

    AgenticServices ..> AgentBuilder : 创建
    AgenticServices ..> UntypedAgentBuilder : 创建
    AgenticServices ..> WorkflowAgentsBuilder : 委托 sequence/parallel/...
```

### 4.2 Agent 与执行链

```mermaid
classDiagram
    class Agent {
        <<annotation>>
        name() String
        description() String
        outputKey() String
        async() boolean
        summarizedContext() String[]
    }

    class AgentInstance {
        <<interface>>
        +type() Class
        +name() String
        +agentId() String
        +description() String
        +outputKey() String
        +arguments() List~AgentArgument~
        +subagents() List
        +topology() AgenticSystemTopology
    }

    class InternalAgent {
        <<interface>>
        +setParent(InternalAgent)
        +listener() AgentListener
    }

    class AgentInvocationHandler {
        -context AiServiceContext
        -builder AgentBuilder
        -agent Object
        +invoke(proxy, method, args) Object
    }

    class AgentExecutor {
        -agentInvoker AgentInvoker
        -agent Object
        +execute(scope, planner) Object
    }

    class Planner {
        <<interface>>
        +init(InitPlanningContext)
        +nextAction(PlanningContext) Action
        +terminated() boolean
        +call(AgentInstance...) Action
        +done() Action
    }

    class Action {
        <<interface>>
        NoOpAction
        AgentCallAction
        DoneAction
        DoneWithResultAction
    }

    AgentBuilder ..> Agent : 读取
    AgentBuilder ..> AgentInvocationHandler : 构建 Proxy
    AgentInvocationHandler ..|> InternalAgent : 实现
    AgentInvocationHandler ..|> AgentInstance : 实现
    AgentExecutor ..|> AgentInstance : 实现
    AgentExecutor ..> Planner : 回调 onSubagentInvoked
    Planner --> Action : 返回
    Action --> AgentInstance : call(agents)
```

### 4.3 AgenticScope 与状态共享

```mermaid
classDiagram
    class AgenticScope {
        <<interface>>
        +memoryId() Object
        +writeState(key, value)
        +readState(key) Object
        +state() Map
        +agentInvocations() List
        +contextAsConversation(agentNames) String
    }

    class DefaultAgenticScope {
        -state Map
        -agentInvocations List
        -context List
        -agents Map
        +getOrCreateAgent(id, factory) T
        +registerAgentInvocation(invocation, agent)
        +rootCallStarted(registry)
        +rootCallEnded(registry, listener)
        +handleError(name, exception) ErrorRecoveryResult
    }

    class AgentInvocation {
        +agentName() String
        +agentId() String
        +output() Object
    }

    class ResultWithAgenticScope {
        +result() T
        +agenticScope() AgenticScope
    }

    AgenticScope <|.. DefaultAgenticScope
    DefaultAgenticScope --> AgentInvocation : 记录
    ResultWithAgenticScope --> AgenticScope : 持有
```

### 4.4 声明式注解与工作流服务

```mermaid
classDiagram
    class SequenceAgent {
        <<annotation>>
        name() String
        description() String
        outputKey() String
        subAgents() Class[]
    }

    class ParallelAgent {
        <<annotation>>
        subAgents() Class[]
    }

    class ConditionalAgent {
        <<annotation>>
        subAgents() Class[]
    }

    class LoopAgent {
        <<annotation>>
        subAgents() Class[]
        exitCondition() Class
    }

    class SupervisorAgent {
        <<annotation>>
        subAgents() Class[]
    }

    class ActivationCondition {
        <<annotation>>
    }

    class SequentialAgentService {
        <<interface>>
        +subAgents(Object...) S
        +outputKey(String) S
        +build() Object
    }

    class SupervisorAgentService {
        <<interface>>
        +subAgents(Object...) S
        +contextGenerationStrategy(Strategy) S
        +responseStrategy(Strategy) S
        +supervisorContext(String) S
        +build() SupervisorAgent
    }

    AgenticServices --> SequentialAgentService : sequenceBuilder
    AgenticServices --> SupervisorAgentService : supervisorBuilder
    DeclarativeUtil ..> SequenceAgent : 解析
    DeclarativeUtil ..> ConditionalAgent : 解析
    ConditionalAgent ..> ActivationCondition : 配合
```

---

## 五、执行流程

所有编排都遵循同一套**执行壳**：入口方法被调用 → 解析参数并写入 **AgenticScope** → 用 **Planner** 驱动循环：每次取 **Action**（`call(agent)` 或 `done(result)`）→ 若是 `call`，则 **AgentExecutor.execute(scope, planner)** 执行子 Agent、把结果写回 Scope、并回调 **planner.onSubagentInvoked** 以得到下一步 Action → 直到 `done`，再根据 **outputKey** 从 Scope 取最终结果并返回。下面按场景用时序图细化。

---

### 5.1 单次调用（叶子 Agent）

单 Agent 由 `AgentBuilder.build()` 生成：内部用 **AiServices** 构造真实实现类，再包一层 **Proxy**，**InvocationHandler** 为 **AgentInvocationHandler**。用户调用接口方法时，最终落到 AiServices 生成的实现类上，从而走 LangChain4j 的 chat/tool 调用链。

**要点**：`invoke` 里会先处理 `AgenticScopeOwner`、`ChatMemoryAccess`、`AgentInstance` 等接口；只有“业务方法”才转发给内部 `agent`（AiServices 实例），由 AiServices 去调 ChatModel / Tools。

```mermaid
sequenceDiagram
    participant Caller as 调用方
    participant Proxy as Agent Proxy
    participant Handler as AgentInvocationHandler
    participant AgentImpl as AiServices 实现
    participant ChatModel as ChatModel/Tools
    participant LLM as LLM

    Caller->>Proxy: 接口方法(args)
    Proxy->>Handler: invoke(proxy, method, args)

    Handler->>Handler: 判断 method 所属接口
    Note over Handler: AgenticScopeOwner / ChatMemory /<br/>AgentInstance / Object 等直接处理

    Handler->>AgentImpl: method.invoke(agent, args)
    Note over AgentImpl: 由 AiServices 生成的实现类

    AgentImpl->>ChatModel: 组请求（system/user/tools）
    ChatModel->>LLM: chat / tool execution
    LLM-->>ChatModel: 响应
    ChatModel-->>AgentImpl: 解析后的返回值
    AgentImpl-->>Handler: result
    Handler-->>Proxy: result
    Proxy-->>Caller: result
```

**流程简述**：Caller → Proxy → AgentInvocationHandler（分支处理）→ 业务方法转发到 AiServices 实现 → ChatModel/Tools → LLM → 原路返回。

---

### 5.2 顺序工作流（Sequence）

顺序工作流由 **PlannerBasedInvocationHandler** 作为入口；其内部 **PlannerLoop** 持有一个 **Planner**（顺序场景下为 **SequentialPlanner**），通过 `firstAction` / `nextAction` 驱动“当前应执行哪个子 Agent”，并由 **AgentExecutor** 执行子 Agent，子 Agent 的返回值写回 Scope，再通过 **onSubagentInvoked** 向 Planner 索取下一步 Action，直到 Planner 返回 `done`。

```mermaid
sequenceDiagram
    participant User as 用户
    participant Proxy as 顺序工作流 Proxy
    participant Handler as PlannerBasedInvocationHandler
    participant Registry as AgenticScopeRegistry
    participant Scope as DefaultAgenticScope
    participant PLoop as PlannerLoop
    participant Planner as SequentialPlanner
    participant Executor as AgentExecutor
    participant SubAgent as 子 Agent

    User->>Proxy: invoke 入参
    Proxy->>Handler: invoke

    Handler->>Handler: executeAgentMethod
    Handler->>Registry: agenticScopeRegistry
    Handler->>Scope: currentAgenticScope
    Note over Scope: 新建或取出 Scope
    Handler->>Scope: writeAgenticScope 写入入参
    Note over Scope: 入参写入 state
    Handler->>Scope: beforeCall
    Handler->>Scope: rootCallStarted

    Handler->>PLoop: PlannerLoop 创建并 run
    Handler->>Planner: init

    PLoop->>Planner: firstAction
    Planner->>Planner: nextAction 返回 call 第一个 agent
    Planner-->>PLoop: Action.call agentExecutor_0

    loop 每一步直到 Action.isDone
        PLoop->>PLoop: agentsToCall 取当前要执行的 agent
        PLoop->>Executor: execute scope this

        Executor->>Scope: 从 state 取参数
        Executor->>SubAgent: invoke
        SubAgent-->>Executor: response
        Executor->>Scope: writeState outputKey response
        Executor->>Scope: registerAgentInvocation
        Executor->>PLoop: onSubagentInvoked

        PLoop->>Planner: nextAction
        alt 还有下一个子 Agent
            Planner->>Planner: cursor 递增并 call 下一个 agent
            Planner-->>PLoop: Action.call agentExecutor_next
        else 已全部执行完
            Planner->>Planner: terminated 返回 done
            Planner-->>PLoop: Action.done
        end
    end

    PLoop->>PLoop: result 从 scope 取 outputKey
    PLoop-->>Handler: result
    Handler->>Scope: rootCallEnded
    Handler-->>Proxy: outputKey 对应值或 result
    Proxy-->>User: 最终返回值
```

**流程简述**：创建/获取 Scope → 入参写入 Scope → PlannerLoop 循环：Planner 返回 `call(下一个 Agent)` → AgentExecutor 执行该 Agent、写 Scope、注册调用记录 → 回调 `onSubagentInvoked` → Planner 返回下一步（继续 `call` 或 `done`）→ 直至 `done`，从 Scope 按 outputKey 取结果并返回。

---

### 5.3 监督者工作流（Supervisor）

监督者与顺序工作流共用 **PlannerBasedInvocationHandler + PlannerLoop**，区别在于 **Planner** 为 **SupervisorPlanner**：每一步不按固定顺序，而是把当前 Scope 中的 request、上一轮子 Agent 的 lastResponse、以及可选摘要/对话历史交给 **PlannerAgent**（由 LLM 实现的“规划器”），由 LLM 决定下一步调用哪个子 Agent（及参数），或返回 **done**。最终结果按 **SupervisorResponseStrategy**（LAST / SUMMARY / SCORED）从 lastResponse 或 done 的 response 中得出。

```mermaid
sequenceDiagram
    participant User as 用户
    participant Proxy as 监督者 Proxy
    participant Handler as PlannerBasedInvocationHandler
    participant Scope as DefaultAgenticScope
    participant PLoop as PlannerLoop
    participant SPlanner as SupervisorPlanner
    participant PAgent as PlannerAgent (LLM)
    participant LLM as LLM
    participant Executor as AgentExecutor
    participant SubAgent as 子 Agent

    User->>Proxy: invoke(request)
    Proxy->>Handler: invoke(...)
    Handler->>Scope: 创建/获取 Scope，writeState("request", request)
    Handler->>PLoop: run

    PLoop->>SPlanner: firstAction(planningContext)
    SPlanner->>SPlanner: nextAction(context)

    loop 直到 agentName 为 done 或达到 maxAgentsInvocations
        SPlanner->>Scope: readState request, previousAgentInvocation.output
        SPlanner->>PAgent: plan(memoryId, agentsList, request, lastResponse, supervisorContext)
        PAgent->>LLM: 构造 prompt（子 Agent 列表 + 当前上下文）
        LLM-->>PAgent: 返回下一步 agentName + arguments 或 done
        PAgent-->>SPlanner: AgentInvocation(agentName, arguments)

        alt agentName 为 done
            SPlanner->>SPlanner: doneAction(scope, lastResponse, done)
            Note over SPlanner: 按 responseStrategy 计算 result：<br/>LAST→lastResponse；SUMMARY→done 的 response；<br/>SCORED→再调 LLM 评分二选一
            SPlanner-->>PLoop: Action.done(result)
        else 调用子 Agent
            SPlanner->>Scope: writeState 写入 LLM 给出的参数
            SPlanner->>SPlanner: findAgentByName(agentName)
            SPlanner-->>PLoop: Action.call(agentInstance)
            PLoop->>Executor: execute(scope, this)
            Executor->>SubAgent: invoke(...)
            SubAgent-->>Executor: response
            Executor->>Scope: writeState(outputKey, response)
            Executor->>PLoop: onSubagentInvoked(invocation)
            PLoop->>SPlanner: nextAction(context)
        end
    end

    PLoop-->>Handler: result
    Handler->>Scope: rootCallEnded(registry, agentListener)
    Handler-->>User: 按 responseStrategy 得到的最终结果
```

**流程简述**：每步 SupervisorPlanner 用 PlannerAgent（LLM）根据当前 request + lastResponse + 可选摘要决定“调用谁 / 传什么参数 / 是否 done” → 若 call 则执行对应 AgentExecutor 并写 Scope，再 nextAction → 若 done 则按 LAST/SUMMARY/SCORED 算最终 result 并结束循环。

---

### 5.4 Human-in-the-Loop 工作流

Human-in-the-Loop 是在**顺序（或其它）工作流**中插入一个“人工步骤”：该步骤对应的“Agent”由 **HumanInTheLoop** 实现，内部不调 LLM，而是通过 **responseProvider.apply(scope)** 向用户展示提示（如用 requestWriter 从 scope 取 inputKey 展示）、阻塞等待用户输入（如 responseReader），再把用户输入写回 Scope 的 outputKey，供后续步骤使用。从编排视角看，它和普通子 Agent 一样被 Planner 以 `call(humanInTheLoopExecutor)` 的方式调度。

```mermaid
sequenceDiagram
    participant User as 用户
    participant Proxy as 工作流 Proxy (含 AI + Human 步骤)
    participant Handler as PlannerBasedInvocationHandler
    participant Scope as DefaultAgenticScope
    participant PLoop as PlannerLoop
    participant Planner as SequentialPlanner
    participant Executor as AgentExecutor
    participant AIAgent as AI 子 Agent
    participant HITL as HumanInTheLoop
    participant Console as 控制台/UI

    User->>Proxy: invoke(inputMap)
    Proxy->>Handler: invoke(...)
    Handler->>Scope: 创建 Scope，写入 input
    Handler->>PLoop: run

    Note over PLoop,Planner: 第一步：AI Agent
    PLoop->>Executor: execute(scope, this) AI 子 Agent
    Executor->>AIAgent: invoke(scope, args)
    AIAgent-->>Executor: modelDecision
    Executor->>Scope: writeState modelDecision, modelDecision
    Executor->>PLoop: onSubagentInvoked(...)
    PLoop->>Planner: nextAction(context)
    Planner-->>PLoop: Action.call(humanValidatorExecutor)

    Note over PLoop,Console: 第二步：人工验证 Agent
    PLoop->>Executor: execute(scope, this) HumanInTheLoop
    Executor->>HITL: invoke 即 askUser(scope)
    HITL->>Scope: readState modelDecision 等
    HITL->>Console: responseProvider.apply(scope) 展示提示文案
    Note over HITL: 例如 requestWriter 打印 AI 建议 + 选项说明
    Console->>User: 显示 请确认最终决定 I/R/H
    User->>Console: 输入 如 I
    Console->>HITL: responseReader.get 用户输入
    HITL-->>Executor: finalDecision
    Executor->>Scope: writeState finalDecision, finalDecision
    Executor->>PLoop: onSubagentInvoked(...)
    PLoop->>Planner: nextAction(context)
    Planner-->>PLoop: Action.done

    PLoop-->>Handler: scope.readState finalDecision
    Handler-->>User: finalDecision
```

**流程简述**：工作流按顺序执行；当某一步是 HumanInTheLoop 时，AgentExecutor 执行的是 `responseProvider.apply(scope)`：从 Scope 取需要展示的内容、向用户展示并阻塞等待输入、将用户输入写回 Scope 的 outputKey，后续步骤与普通 Agent 一致（从 Scope 读、写，Planner 继续 nextAction 直至 done）。

---

### 5.5 声明式 createAgenticSystem 流程

通过 **AgenticServices.createAgenticSystem(agentServiceClass)** 或重载（带 ChatModel、agentConfigurator）创建 Agent 时，会先尝试按**声明式注解**解析该类：若存在 `@SequenceAgent` / `@ParallelAgent` / `@ConditionalAgent` 等，则走 **createComposedAgent**，递归为子 Agent 类创建实例并交给 WorkflowAgentsBuilder 组装成顺序/并行/条件等复合 Agent；否则退化为 **agentBuilder(agentServiceClass).build()**，得到单个叶子 Agent（Proxy + AgentInvocationHandler）。

```mermaid
flowchart LR
    A[createAgenticSystem] --> B{有声明式注解?}
    B -->|是| C[createComposedAgent]
    B -->|否| D[agentBuilder.build]
    C --> E[DeclarativeUtil 解析注解]
    E --> F[递归创建子 Agent]
    F --> G[WorkflowAgentsBuilder 建 Sequence 等]
    G --> H[返回复合 Agent 实例]
    D --> I[AgentBuilder 加 Proxy]
    I --> J[返回单 Agent 实例]
```

---

## 六、关键概念小结

| 概念 | 说明 |
|------|------|
| **AgenticScope** | 一次“根调用”内的共享状态；子 Agent 的 input/output 通过 `readState`/`writeState` 传递。 |
| **Planner** | 决定下一步执行哪个 Agent、何时结束；Sequence/Parallel/Conditional/Loop/Supervisor 各有实现。 |
| **AgentInstance** | 对“可被编排的一步”的抽象，既可能是叶子 Agent（AI），也可能是复合 Agent（带 subagents）。 |
| **Action** | Planner 返回的下一步：`call(agent(s))` 或 `done(result)`。 |
| **outputKey** | 子 Agent 的返回值写入 Scope 的键，供后续 Agent 或最终输出使用。 |
| **SupervisorContextStrategy** | 监督者如何把“已执行过的子 Agent 对话”带给 LLM：CHAT_MEMORY / SUMMARIZATION / CHAT_MEMORY_AND_SUMMARIZATION。 |
| **SupervisorResponseStrategy** | 监督者最终返回什么：LAST（最后一环输出）/ SUMMARY（done 时的总结）/ SCORED（用 LLM 评分二选一）。 |

---

## 七、参考链接

- 框架源码：[langchain4j/langchain4j-agentic](https://github.com/langchain4j/langchain4j/tree/main/langchain4j-agentic)
- 示例工程：[langchain4j-examples/agentic-tutorial](https://github.com/langchain4j/langchain4j-examples/tree/main/agentic-tutorial)
- 
