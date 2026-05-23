package com.ruoyi.system.agent;

import com.ruoyi.system.domain.AiConversationMessage;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * 提示词构建器 — 组装发送给LLM的完整消息数组
 *
 * Phase 4 升级: 暴露独立的消息构建方法，支持 Function Calling 循环中的动态消息拼接
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public class PromptBuilder {

    /**
     * 构建完整的消息数组 (OpenAI兼容格式)
     */
    public JSONArray buildMessages(AgentContext context, String summary) {
        JSONArray messages = new JSONArray();

        messages.add(buildSystemMessageObj(context, summary));

        if (context.getKnowledgeContext() != null && !context.getKnowledgeContext().isEmpty()) {
            messages.add(buildKnowledgeContextObj(context.getKnowledgeContext()));
        }

        if (summary != null && !summary.isEmpty()) {
            JSONObject summaryMsg = buildSummaryMessageObj(summary);
            if (summaryMsg != null) {
                messages.add(summaryMsg);
            }
        }

        List<AiConversationMessage> history = context.getHistory();
        if (history != null) {
            for (AiConversationMessage msg : history) {
                messages.add(buildHistoryMessageObj(msg));
            }
        }

        messages.add(buildUserMessageObj(context.getUserMessage()));

        return messages;
    }

    /**
     * 构建用于摘要生成的简单消息数组
     */
    public JSONArray buildSummaryRequest(String earlyMessagesText) {
        JSONArray messages = new JSONArray();

        JSONObject sysMsg = new JSONObject();
        sysMsg.put("role", "system");
        sysMsg.put("content", "你是一个对话摘要助手。请将对话历史压缩为简洁的摘要。");
        messages.add(sysMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", "请将以下对话历史压缩为一段简洁的摘要，保留关键信息、用户偏好和重要事实，不超过200字：\n\n" + earlyMessagesText);
        messages.add(userMsg);

        return messages;
    }

    // ==================== Phase 4: 独立消息构建方法 ====================

    /**
     * 构建系统消息JSONObject
     */
    public JSONObject buildSystemMessageObj(AgentContext context, String summary) {
        JSONObject msg = new JSONObject();
        msg.put("role", "system");

        StringBuilder content = new StringBuilder();
        if (context.getSystemPrompt() != null && !context.getSystemPrompt().isEmpty()) {
            content.append(context.getSystemPrompt());
        } else {
            content.append("你是一个AI校园墙智能助手，可以回答校园相关的问题，帮助用户解决校园生活中的各种需求。");
        }

        // 工具使用说明
        if (content.indexOf("工具") == -1) {
            content.append("\n\n你可以使用提供的工具来获取实时信息、查询知识库、审核内容等。");
            content.append("当用户的问题需要实时信息时，请使用 web_search 工具搜索互联网。");
            content.append("当用户询问校园相关规则制度时，请使用 kb_query 工具查询知识库。");
        }

        if (summary != null && !summary.isEmpty()) {
            content.append("\n\n【历史对话摘要】\n").append(summary);
        }

        msg.put("content", content.toString());
        return msg;
    }

    /**
     * 构建知识库上下文消息JSONObject
     */
    public JSONObject buildKnowledgeContextObj(String kbContext) {
        JSONObject msg = new JSONObject();
        msg.put("role", "system");
        msg.put("content", "【参考知识库内容】\n\n" + kbContext + "\n\n请基于以上参考内容回答用户问题。如果参考内容不足以回答问题，可以结合你的知识进行补充，但要明确说明。");
        return msg;
    }

    /**
     * 构建摘要消息JSONObject
     */
    public JSONObject buildSummaryMessageObj(String summary) {
        if (summary == null || summary.isEmpty()) return null;
        JSONObject msg = new JSONObject();
        msg.put("role", "system");
        msg.put("content", "【历史对话摘要】\n" + summary);
        return msg;
    }

    /**
     * 构建历史消息JSONObject
     */
    public JSONObject buildHistoryMessageObj(AiConversationMessage msg) {
        JSONObject jsonMsg = new JSONObject();
        jsonMsg.put("role", msg.getRole());

        if ("tool".equals(msg.getRole()) && msg.getToolCalls() != null) {
            try {
                jsonMsg.put("content", msg.getContent());
                JSONObject toolCalls = JSONObject.parseObject(msg.getToolCalls());
                if (toolCalls.containsKey("tool_call_id")) {
                    jsonMsg.put("tool_call_id", toolCalls.getString("tool_call_id"));
                }
                if (toolCalls.containsKey("name")) {
                    jsonMsg.put("name", toolCalls.getString("name"));
                }
            } catch (Exception e) {
                jsonMsg.put("content", msg.getContent());
            }
        } else if ("assistant".equals(msg.getRole()) && msg.getToolCalls() != null) {
            // assistant 消息可能包含 tool_calls
            jsonMsg.put("content", msg.getContent() != null ? msg.getContent() : null);
            try {
                JSONArray toolCalls = JSONArray.parse(msg.getToolCalls());
                jsonMsg.put("tool_calls", toolCalls);
            } catch (Exception e) {
                jsonMsg.put("content", msg.getContent());
            }
        } else {
            jsonMsg.put("content", msg.getContent());
        }

        return jsonMsg;
    }

    /**
     * 构建用户消息JSONObject
     */
    public JSONObject buildUserMessageObj(String userMessage) {
        JSONObject msg = new JSONObject();
        msg.put("role", "user");
        msg.put("content", userMessage);
        return msg;
    }
}
