package com.ruoyi.system.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.agent.tool.ToolRegistry;
import com.ruoyi.system.agent.tool.ToolResult;
import com.ruoyi.system.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * Agent引擎 — 编排Agent核心流程
 *
 * Phase 4 升级: 支持 Function Calling 多轮工具调用循环
 *
 * 流程:
 * 1. 加载会话上下文(系统提示词+历史+知识库)
 * 2. 记忆管理(窗口检查+摘要压缩)
 * 3. 构建提示词消息数组(含tools)
 * 4. Function Calling 循环(最多5轮):
 *    a. 调用LLM同步获取响应
 *    b. 如有tool_calls: 发送tool_call SSE事件 → 执行工具 → 发送tool_result SSE事件 → 继续循环
 *    c. 如有content: 流式输出最终回复
 * 5. 保存消息+更新会话
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public class AgentEngine {

    private static final Logger log = LoggerFactory.getLogger(AgentEngine.class);

    /** Function Calling 最大轮次 */
    private static final int MAX_FC_ROUNDS = 5;

    private final MemoryManager memoryManager;
    private final PromptBuilder promptBuilder;
    private final LlmCaller llmCaller;
    private final ToolRegistry toolRegistry;

    public AgentEngine(MemoryManager memoryManager, PromptBuilder promptBuilder,
                       LlmCaller llmCaller, ToolRegistry toolRegistry) {
        this.memoryManager = memoryManager;
        this.promptBuilder = promptBuilder;
        this.llmCaller = llmCaller;
        this.toolRegistry = toolRegistry;
    }

    /**
     * 执行对话
     *
     * @param context       Agent上下文
     * @param emitter       SSE发射器
     * @param callback      完成回调
     */
    public void execute(AgentContext context, SseEmitter emitter, AgentCallback callback) {
        try {
            // ========== 1. 记忆管理 ==========
            List<AiConversationMessage> allHistory = context.getHistory();
            String summary = null;

            if (allHistory != null && memoryManager.needCompression(allHistory)) {
                String earlyText = memoryManager.getEarlyMessagesForSummary(allHistory);
                if (earlyText != null) {
                    JSONArray summaryReq = promptBuilder.buildSummaryRequest(earlyText);
                    try {
                        summary = llmCaller.generateSummary(context, summaryReq);
                    } catch (Exception e) {
                        log.warn("摘要生成失败", e);
                    }
                }
            }

            // ========== 2. 获取tools ==========
            JSONArray tools = null;
            if (toolRegistry != null && toolRegistry.hasTools()) {
                tools = toolRegistry.getToolsForLLM();
            }

            // ========== 3. 保存用户消息 ==========
            AiConversationMessage userMsg = new AiConversationMessage();
            userMsg.setConversationId(context.getConversationId());
            userMsg.setRole("user");
            userMsg.setContent(context.getUserMessage());
            userMsg.setTokenCount(memoryManager.estimateTokens(context.getUserMessage()));
            userMsg.setCreateTime(new Date());
            callback.onUserMessage(userMsg);

            // ========== 4. 构建会话消息列表(用于FC循环) ==========
            List<JSONObject> conversationMessages = new ArrayList<>();
            conversationMessages.add(promptBuilder.buildSystemMessageObj(context, summary));
            if (context.getKnowledgeContext() != null && !context.getKnowledgeContext().isEmpty()) {
                conversationMessages.add(promptBuilder.buildKnowledgeContextObj(context.getKnowledgeContext()));
            }
            if (summary != null && !summary.isEmpty()) {
                conversationMessages.add(promptBuilder.buildSummaryMessageObj(summary));
            }
            if (allHistory != null) {
                for (AiConversationMessage msg : allHistory) {
                    conversationMessages.add(promptBuilder.buildHistoryMessageObj(msg));
                }
            }
            conversationMessages.add(promptBuilder.buildUserMessageObj(context.getUserMessage()));

            final String finalSummary = summary;

            // ========== 5. Function Calling 循环 ==========
            JSONObject finalLlmResponse = null;
            List<ToolCallRecord> executedToolCalls = new ArrayList<>();

            for (int round = 0; round < MAX_FC_ROUNDS; round++) {
                JSONArray messages = new JSONArray();
                messages.addAll(conversationMessages);

                JSONObject llmResponse = llmCaller.chatSyncWithTools(context, messages, tools);
                if (llmResponse == null) {
                    sendError(emitter, "LLM返回为空");
                    return;
                }

                String content = LlmCaller.extractContent(llmResponse);
                JSONArray toolCalls = LlmCaller.extractToolCalls(llmResponse);

                // 有 tool_calls → 执行工具并继续循环
                if (toolCalls != null && !toolCalls.isEmpty()) {
                    log.info("Function Calling 第{}轮: LLM请求调用{}个工具", round + 1, toolCalls.size());

                    // 添加 assistant 消息(含 tool_calls)
                    JSONObject assistantMsg = buildAssistantToolCallMessage(content, toolCalls);
                    conversationMessages.add(assistantMsg);

                    // 执行每个工具
                    for (int i = 0; i < toolCalls.size(); i++) {
                        JSONObject tc = toolCalls.getJSONObject(i);
                        String toolCallId = tc.getString("id");
                        JSONObject function = tc.getJSONObject("function");
                        String funcName = function.getString("name");
                        String funcArgsStr = function.getString("arguments");

                        // 发送 tool_call SSE 事件
                        JSONObject toolCallEvent = new JSONObject();
                        toolCallEvent.put("id", toolCallId);
                        toolCallEvent.put("name", funcName);
                        try {
                            toolCallEvent.put("arguments", JSON.parseObject(funcArgsStr));
                        } catch (Exception e) {
                            toolCallEvent.put("arguments", funcArgsStr);
                        }
                        sendToolEvent(emitter, "tool_call", toolCallEvent);

                        // 执行工具
                        ToolResult toolResult;
                        try {
                            JSONObject args = JSON.parseObject(funcArgsStr);
                            toolResult = toolRegistry.execute(funcName, args);
                        } catch (Exception e) {
                            toolResult = ToolResult.error("参数解析失败: " + e.getMessage());
                        }

                        // 发送 tool_result SSE 事件
                        JSONObject toolResultEvent = new JSONObject();
                        toolResultEvent.put("id", toolCallId);
                        toolResultEvent.put("name", funcName);
                        toolResultEvent.put("success", toolResult.isSuccess());
                        toolResultEvent.put("content", toolResult.getContent());
                        sendToolEvent(emitter, "tool_result", toolResultEvent);

                        // 添加 tool 消息
                        JSONObject toolMsg = new JSONObject();
                        toolMsg.put("role", "tool");
                        toolMsg.put("tool_call_id", toolCallId);
                        toolMsg.put("name", funcName);
                        toolMsg.put("content", toolResult.getContent());
                        conversationMessages.add(toolMsg);

                        // 记录工具调用
                        executedToolCalls.add(new ToolCallRecord(toolCallId, funcName,
                                funcArgsStr, toolResult));
                    }

                    // 保存 assistant tool_call 消息到 DB
                    AiConversationMessage toolAskMsg = new AiConversationMessage();
                    toolAskMsg.setConversationId(context.getConversationId());
                    toolAskMsg.setRole("assistant");
                    if (content != null && !content.isEmpty()) {
                        toolAskMsg.setContent(content);
                    }
                    toolAskMsg.setToolCalls(JSON.toJSONString(toolCalls));
                    toolAskMsg.setTokenCount(0);
                    toolAskMsg.setCreateTime(new Date());
                    callback.onToolCallMessage(toolAskMsg);

                    // 保存 tool 结果消息到 DB
                    for (ToolCallRecord record : executedToolCalls) {
                        AiConversationMessage toolResultMsg = new AiConversationMessage();
                        toolResultMsg.setConversationId(context.getConversationId());
                        toolResultMsg.setRole("tool");
                        toolResultMsg.setContent(record.result.getContent());
                        JSONObject meta = new JSONObject();
                        meta.put("tool_call_id", record.id);
                        meta.put("name", record.name);
                        toolResultMsg.setToolCalls(meta.toString());
                        toolResultMsg.setTokenCount(0);
                        toolResultMsg.setCreateTime(new Date());
                        callback.onToolMessage(toolResultMsg, record.name);
                    }

                    finalLlmResponse = llmResponse;
                    continue; // 继续下一轮
                }

                // 有文字内容 → 这是最终回复
                if (content != null && !content.isEmpty()) {
                    finalLlmResponse = llmResponse;

                    // 添加 assistant 消息
                    JSONObject finalAssistantMsg = new JSONObject();
                    finalAssistantMsg.put("role", "assistant");
                    finalAssistantMsg.put("content", content);
                    conversationMessages.add(finalAssistantMsg);

                    // 流式输出内容
                    streamContent(emitter, content);

                    // 保存最终助手消息
                    AiConversationMessage assistantMsg = new AiConversationMessage();
                    assistantMsg.setConversationId(context.getConversationId());
                    assistantMsg.setRole("assistant");
                    assistantMsg.setContent(content);
                    assistantMsg.setTokenCount(memoryManager.estimateTokens(content));
                    assistantMsg.setCreateTime(new Date());
                    callback.onAssistantMessage(assistantMsg, finalSummary);
                    return;
                }

                // 无内容无tool_calls → 关闭
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
                return;
            }

            // ========== 超过最大轮次 → 流式输出最终结果 ==========
            if (finalLlmResponse != null) {
                String finalContent = LlmCaller.extractContent(finalLlmResponse);
                if (finalContent != null && !finalContent.isEmpty()) {
                    streamContent(emitter, finalContent);

                    AiConversationMessage assistantMsg = new AiConversationMessage();
                    assistantMsg.setConversationId(context.getConversationId());
                    assistantMsg.setRole("assistant");
                    assistantMsg.setContent(finalContent);
                    assistantMsg.setTokenCount(memoryManager.estimateTokens(finalContent));
                    assistantMsg.setCreateTime(new Date());
                    callback.onAssistantMessage(assistantMsg, finalSummary);
                    return;
                }
            }

            sendError(emitter, "Agent达到最大处理轮次，未收到有效回复");
            emitter.complete();

        } catch (Exception e) {
            log.error("Agent执行异常", e);
            sendError(emitter, "Agent执行异常: " + e.getMessage());
        }
    }

    /**
     * 获取MemoryManager (供外部使用)
     */
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 构建 assistant 消息（含 tool_calls，不带content）
     */
    private JSONObject buildAssistantToolCallMessage(String content, JSONArray toolCalls) {
        JSONObject msg = new JSONObject();
        msg.put("role", "assistant");
        if (content != null && !content.isEmpty()) {
            msg.put("content", content);
        } else {
            msg.put("content", (Object) null);
        }
        msg.put("tool_calls", toolCalls);
        return msg;
    }

    /**
     * 发送工具 SSE 事件
     */
    private void sendToolEvent(SseEmitter emitter, String eventName, JSONObject data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data.toJSONString()));
        } catch (IOException e) {
            log.warn("发送SSE工具事件失败: {}", eventName, e);
        }
    }

    /**
     * 流式输出文本内容
     */
    private void streamContent(SseEmitter emitter, String content) {
        try {
            // 按字符逐字输出(模拟流式效果)
            for (int i = 0; i < content.length(); i++) {
                String ch = String.valueOf(content.charAt(i));
                emitter.send(SseEmitter.event().name("message").data(ch));
                // 控制速度，模拟真实流式
                if (i % 3 == 0) {
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                }
            }
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();
        } catch (IOException e) {
            log.warn("流式输出失败", e);
            try { emitter.complete(); } catch (IOException ignored) {}
        }
    }

    /**
     * 发送错误事件
     */
    private void sendError(SseEmitter emitter, String errorMsg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(errorMsg));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    // ==================== 内部类 ====================

    /**
     * 工具调用记录
     */
    private static class ToolCallRecord {
        final String id;
        final String name;
        final String arguments;
        final ToolResult result;

        ToolCallRecord(String id, String name, String arguments, ToolResult result) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
            this.result = result;
        }
    }

    /**
     * Agent回调接口 (Phase 4 升级: 增加工具消息回调)
     */
    public interface AgentCallback {
        /** 用户消息已创建 */
        void onUserMessage(AiConversationMessage userMsg);

        /** 工具调用请求消息 (role=assistant, 含 tool_calls) */
        default void onToolCallMessage(AiConversationMessage assistantToolCallMsg) {}

        /** 工具执行结果消息 (role=tool) */
        default void onToolMessage(AiConversationMessage toolResultMsg, String toolName) {}

        /** 助手消息已生成(含完整内容) */
        void onAssistantMessage(AiConversationMessage assistantMsg, String summary);
    }
}
