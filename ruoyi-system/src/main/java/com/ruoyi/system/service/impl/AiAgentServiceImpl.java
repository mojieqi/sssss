package com.ruoyi.system.service.impl;

import com.ruoyi.system.agent.*;
import com.ruoyi.system.agent.tool.ToolRegistry;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IAiAgentService;
import com.ruoyi.system.service.IEmbeddingService;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent核心Service实现
 *
 * 编排Agent引擎、会话管理、知识库检索、消息持久化
 *
 * Phase 4 升级: 注入ToolRegistry，支持 Function Calling 工具调用
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Service
public class AiAgentServiceImpl implements IAiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentServiceImpl.class);

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiConversationMessageMapper messageMapper;

    @Autowired
    private AiPromptTemplateMapper promptTemplateMapper;

    @Autowired
    private AiLlmConfigMapper llmConfigMapper;

    @Autowired
    private AiMessageKbRefMapper kbRefMapper;

    @Autowired
    private IEmbeddingService embeddingService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private ToolRegistry toolRegistry;

    /** SseEmitter 超时时间(毫秒) */
    private static final long SSE_TIMEOUT = 300000L;

    /** RAG上下文最大字符数 */
    private static final int RAG_MAX_CHARS = 2000;

    @Override
    public SseEmitter chat(Long conversationId, String userMessage) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 1. 加载会话
        AiConversation conversation = conversationMapper.selectAiConversationById(conversationId);
        if (conversation == null) {
            sendEmitterError(emitter, "会话不存在");
            return emitter;
        }

        // 2. 加载LLM配置
        AiLlmConfig llmConfig = llmConfigMapper.selectAiLlmConfigById(conversation.getLlmConfigId());
        if (llmConfig == null || StringUtils.isEmpty(llmConfig.getApiKey())) {
            sendEmitterError(emitter, "LLM配置无效，API Key未设置");
            return emitter;
        }

        // 3. 加载系统提示词
        String systemPrompt = loadSystemPrompt(conversation);

        // 4. 加载历史消息
        List<AiConversationMessage> history = messageMapper.selectAiConversationMessageList(
                new AiConversationMessage() {{ setConversationId(conversationId); }}
        );

        // 5. 知识库检索 (RAG)
        String knowledgeContext = searchKnowledgeBase(conversation, userMessage);

        // 6. 构建Agent上下文
        AgentContext context = AgentContext.builder()
                .conversationId(conversationId)
                .systemPrompt(systemPrompt)
                .history(history)
                .knowledgeContext(knowledgeContext)
                .userMessage(userMessage)
                .llmConfigId(llmConfig.getConfigId())
                .modelName(llmConfig.getDefaultModel())
                .baseUrl(llmConfig.getBaseUrl())
                .apiKey(llmConfig.getApiKey())
                .maxTokens(conversation.getMaxTokens())
                .temperature(0.7)
                .build();

        // 7. 创建Agent引擎并执行
        AgentEngine engine = createEngine();
        engine.execute(context, emitter, new AgentEngine.AgentCallback() {
            @Override
            public void onUserMessage(AiConversationMessage userMsg) {
                messageMapper.insertAiConversationMessage(userMsg);
            }

            @Override
            public void onToolCallMessage(AiConversationMessage assistantToolCallMsg) {
                // 保存 assistant 的 tool_calls 消息
                messageMapper.insertAiConversationMessage(assistantToolCallMsg);
            }

            @Override
            public void onToolMessage(AiConversationMessage toolResultMsg, String toolName) {
                // 保存 tool 角色消息
                messageMapper.insertAiConversationMessage(toolResultMsg);
                log.debug("保存工具结果消息: {} -> {}", toolName,
                        toolResultMsg.getContent() != null
                                ? toolResultMsg.getContent().substring(0, Math.min(50, toolResultMsg.getContent().length()))
                                : "");
            }

            @Override
            public void onAssistantMessage(AiConversationMessage assistantMsg, String summary) {
                messageMapper.insertAiConversationMessage(assistantMsg);
                conversationMapper.updateMessageCount(conversationId);

                // 更新会话标题(首次对话时)
                if (history.isEmpty() && conversation.getTitle().equals("新对话")) {
                    AiConversation update = new AiConversation();
                    update.setConversationId(conversationId);
                    String title = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
                    update.setTitle(title);
                    conversationMapper.updateAiConversation(update);
                }
            }
        });

        // SSE 生命周期
        emitter.onTimeout(() -> {});
        emitter.onError(ex -> {});
        emitter.onCompletion(() -> {});

        return emitter;
    }

    @Override
    public List<AiConversationMessage> getMessages(Long conversationId) {
        AiConversationMessage query = new AiConversationMessage();
        query.setConversationId(conversationId);
        return messageMapper.selectAiConversationMessageList(query);
    }

    @Override
    public int saveMessage(AiConversationMessage message) {
        return messageMapper.insertAiConversationMessage(message);
    }

    // --- private helpers ---

    private AgentEngine createEngine() {
        return new AgentEngine(
                new MemoryManager(),
                new PromptBuilder(),
                new LlmCaller(restTemplate),
                toolRegistry
        );
    }

    private void sendEmitterError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(msg));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private String loadSystemPrompt(AiConversation conversation) {
        if (conversation.getPromptTemplateId() != null) {
            AiPromptTemplate template = promptTemplateMapper.selectAiPromptTemplateById(
                    conversation.getPromptTemplateId());
            if (template != null && "0".equals(template.getStatus()) &&
                    StringUtils.isNotEmpty(template.getPromptContent())) {
                return template.getPromptContent();
            }
        }
        return null;
    }

    private String searchKnowledgeBase(AiConversation conversation, String userMessage) {
        if (conversation.getKbId() == null) return null;

        try {
            List<IEmbeddingService.ChunkSearchResult> results =
                    embeddingService.searchInBase(conversation.getKbId(), userMessage, 5);
            if (results.isEmpty()) return null;
            return embeddingService.buildKnowledgeContext(results, RAG_MAX_CHARS);
        } catch (Exception e) {
            return null;
        }
    }
}
