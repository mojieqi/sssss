<template>
  <div class="agent-container">
    <!-- 左侧会话列表 -->
    <ConversationSidebar
      :conversations="conversationList"
      :activeId="activeConversationId"
      :loading="convLoading"
      @select="handleSelectConversation"
      @delete="handleDeleteConversation"
      @new-conversation="handleNewConversation"
    />

    <!-- 中间对话区 -->
    <ChatWindow
      ref="chatWindow"
      :messages="messages"
      :streaming="streaming"
      :streamContent="streamContent"
      :toolCalls="toolCalls"
      @send="handleSendMessage"
      @stop="handleStop"
    />

    <!-- 右侧设置面板 -->
    <AgentSettings
      :conversation="activeConversation"
      :configs="llmConfigs"
      :prompts="systemPrompts"
      :knowledgeBases="knowledgeBases"
      @update-settings="handleUpdateSettings"
      @clear-context="handleClearContext"
    />
  </div>
</template>

<script>
import ConversationSidebar from './components/ConversationSidebar.vue'
import ChatWindow from './components/ChatWindow.vue'
import AgentSettings from './components/AgentSettings.vue'
import {
  listConversations, createConversation, deleteConversation,
  getMessages, chatStream, stopChat, clearConversation
} from '@/api/ai/agent'
import { listConfig } from '@/api/ai/llmConfig'
import { listSystemPrompts } from '@/api/ai/promptTemplate'
import { listKnowledgeBase } from '@/api/ai/knowledge'

export default {
  name: 'AiAgent',
  components: { ConversationSidebar, ChatWindow, AgentSettings },
  data() {
    return {
      // 会话列表
      conversationList: [],
      convLoading: false,
      activeConversationId: null,
      activeConversation: {},

      // 消息
      messages: [],

      // 流式状态
      streaming: false,
      streamContent: '',
      currentReader: null,
      abortController: null,

      // 工具调用状态（Phase 4）：流式过程中实时展示的工具调用卡片
      toolCalls: [],

      // 选项数据
      llmConfigs: [],
      systemPrompts: [],
      knowledgeBases: []
    }
  },
  created() {
    this.loadOptions()
    this.loadConversations()
  },
  methods: {
    // ==================== 初始化加载 ====================
    async loadOptions() {
      try {
        const [configRes, promptRes, kbRes] = await Promise.all([
          listConfig({ status: '0' }),
          listSystemPrompts(),
          listKnowledgeBase()
        ])
        this.llmConfigs = configRes.rows || []
        this.systemPrompts = promptRes.data || []
        this.knowledgeBases = kbRes.rows || []
      } catch (e) {
        console.error('加载选项失败', e)
      }
    },

    async loadConversations() {
      this.convLoading = true
      try {
        const res = await listConversations({})
        this.conversationList = res.rows || []
        if (this.conversationList.length > 0 && !this.activeConversationId) {
          this.handleSelectConversation(this.conversationList[0])
        }
      } finally {
        this.convLoading = false
      }
    },

    // ==================== 会话操作 ====================
    async handleNewConversation() {
      const defaultConfig = this.llmConfigs.length > 0 ? this.llmConfigs[0].configId : null
      if (!defaultConfig) {
        this.$message.warning('没有可用的LLM配置，请先在【LLM大模型配置】中添加')
        return
      }

      try {
        const res = await createConversation({
          llmConfigId: defaultConfig,
          title: '新对话',
          memoryMode: '1',
          maxTokens: 4096
        })
        this.$message.success('新建会话成功')
        await this.loadConversations()
        const newConv = this.conversationList.find(c => c.conversationId === res.data?.conversationId)
        if (newConv) {
          this.handleSelectConversation(newConv)
        }
      } catch (e) {
        this.$message.error('新建会话失败')
      }
    },

    async handleSelectConversation(conv) {
      this.activeConversationId = conv.conversationId
      this.activeConversation = conv
      this.messages = []
      try {
        const res = await getMessages(conv.conversationId)
        this.messages = res.data || []
        this.$nextTick(() => {
          this.$refs.chatWindow?.scrollToBottom()
        })
      } catch (e) {
        console.error('加载消息失败', e)
      }
    },

    async handleDeleteConversation(id) {
      try {
        await deleteConversation(id)
        this.$message.success('删除成功')
        if (this.activeConversationId === id) {
          this.activeConversationId = null
          this.activeConversation = {}
          this.messages = []
        }
        await this.loadConversations()
      } catch (e) {
        this.$message.error('删除失败')
      }
    },

    async handleClearContext() {
      if (!this.activeConversationId) return
      try {
        await this.$confirm('确定清空当前会话的所有消息吗？', '提示', { type: 'warning' })
        await clearConversation(this.activeConversationId)
        this.messages = []
        this.$message.success('上下文已清空')
      } catch (e) {
        // 取消
      }
    },

    async handleUpdateSettings(settings) {
      if (!this.activeConversationId) return
      Object.assign(this.activeConversation, settings)
    },

    // ==================== 消息发送 (Phase 4 升级) ====================
    async handleSendMessage(text) {
      if (!this.activeConversationId) {
        this.$message.warning('请先选择或创建一个会话')
        return
      }
      if (this.streaming) return

      // 添加用户消息到界面
      this.messages.push({
        _tempId: Date.now(),
        role: 'user',
        content: text,
        createTime: new Date().toISOString()
      })

      // 开始流式
      this.streaming = true
      this.streamContent = ''
      this.toolCalls = []

      try {
        const response = await chatStream(this.activeConversationId, text)

        if (!response.ok) {
          throw new Error('请求失败: ' + response.status)
        }

        const reader = response.body.getReader()
        this.currentReader = reader
        const decoder = new TextDecoder()
        let buffer = ''
        let streamDone = false
        let currentEventType = 'message'

        while (!streamDone) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            // 解析SSE event类型
            if (line.startsWith('event:')) {
              currentEventType = line.substring(6).trim()
              continue
            }

            if (line.startsWith('data:')) {
              const data = line.substring(5).trim()

              if (data === '[DONE]') {
                streamDone = true
                break
              }

              if (!data) continue

              // Phase 4: 处理工具调用事件
              if (currentEventType === 'tool_call') {
                this.handleToolCallEvent(data)
              } else if (currentEventType === 'tool_result') {
                this.handleToolResultEvent(data)
              } else {
                // 默认: message 事件 — 流式文本
                this.streamContent += data
              }
            }

            // 重置事件类型（data行处理完后）
            if (line.startsWith('data:')) {
              currentEventType = 'message'
            }
          }
        }

        // 添加AI消息
        if (this.streamContent) {
          this.messages.push({
            _tempId: Date.now(),
            role: 'assistant',
            content: this.streamContent,
            createTime: new Date().toISOString()
          })
        }
      } catch (e) {
        if (e.name !== 'AbortError') {
          this.$message.error('对话失败: ' + e.message)
        }
      } finally {
        this.streaming = false
        this.streamContent = ''
        this.toolCalls = []
        this.currentReader = null
        // 刷新消息列表和会话列表
        if (this.activeConversationId) {
          const res = await getMessages(this.activeConversationId)
          this.messages = res.data || []
        }
        // 异步刷新侧边栏(不影响主流程)
        this.loadConversations()
      }
    },

    /** Phase 4: 处理 tool_call SSE 事件 */
    handleToolCallEvent(data) {
      try {
        const tc = JSON.parse(data)
        const existing = this.toolCalls.find(t => t.id === tc.id)
        if (existing) return

        this.toolCalls.push({
          id: tc.id,
          name: tc.name,
          arguments: tc.arguments,
          success: true,
          pending: true,
          role: 'tool_call',
          _tempId: 'tc-' + tc.id
        })
      } catch (e) {
        console.error('解析tool_call事件失败', e)
      }
    },

    /** Phase 4: 处理 tool_result SSE 事件 */
    handleToolResultEvent(data) {
      try {
        const tr = JSON.parse(data)
        const existing = this.toolCalls.find(t => t.id === tr.id)
        if (existing) {
          existing.pending = false
          existing.success = tr.success !== false
          existing.content = tr.content || '工具执行完成'
        }
      } catch (e) {
        console.error('解析tool_result事件失败', e)
      }
    },

    handleStop() {
      if (this.currentReader) {
        this.currentReader.cancel()
        this.currentReader = null
      }
      this.streaming = false
      this.toolCalls = []
      if (this.streamContent) {
        this.messages.push({
          _tempId: Date.now(),
          role: 'assistant',
          content: this.streamContent + '(已停止生成)',
          createTime: new Date().toISOString()
        })
      }
      this.streamContent = ''
    }
  }
}
</script>

<style scoped>
.agent-container {
  display: flex;
  height: calc(100vh - 84px);
  background: #fff;
}
</style>
