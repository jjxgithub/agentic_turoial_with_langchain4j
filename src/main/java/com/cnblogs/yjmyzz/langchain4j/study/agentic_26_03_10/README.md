# agentic_26_03_10：统一对话入口（澄清 + 编排，REST + SSE）

**仅一个入口**：先判断用户意图是否清晰，不清晰则让用户补充（多轮澄清），清晰则自动做子问题拆分与编排（A/B 并发、C 依赖 A 和 B 等），并通过 SSE 推送进度。

## 启动

运行主类 `AgentDesignPatternApplication`，启动后默认端口 8080。

---

## 唯一接口：POST `/api/chat/stream`

- **Content-Type**: `application/json`，**Accept**: `text/event-stream`
- **请求体**: `{ "sessionId": "唯一会话ID", "input": "用户当前输入" }`

**响应（SSE）分两种情形：**

1. **意图不清晰**（需用户补充）
   - `clarification`：澄清问题（data 为一句问话）
   - `done`
   - 客户端可用同一 `sessionId` 再次 POST 用户补充内容，直至意图清晰。

2. **意图清晰**（自动进入编排）
   - `intent_clear`：意图摘要（data 为模型归纳的一句话，用于后续拆分）
   - `plan`：拆分后的计划 JSON（tasks + dependsOn）
   - `task_start` / `task_result` / `task_end`：各子任务执行进度
   - `plan_done`：全部完成，data 为结果汇总

同一 `sessionId` 共享对话记忆，模型会区分「上一问的补充」与「新问题」。

---

## 调用示例

```bash
# 第一次：模糊输入 → 可能收到 clarification
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" -H "Accept: text/event-stream" \
  -d '{"sessionId":"user-001","input":"我想查一下"}'

# 同一 sessionId 补充后 → 意图清晰则收到 intent_clear + plan + task_* + plan_done
curl -N -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" -H "Accept: text/event-stream" \
  -d '{"sessionId":"user-001","input":"查订单状态、账户余额，并给复购建议"}'
```
