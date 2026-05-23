# AI Agent 智能体开发 — 任务规划

**版本：** 1.0.0  
**日期：** 2026-05-23  
**状态：** 规划中  
**项目：** AI校园墙智能平台

---

## 一、项目背景

本项目为 **AI校园墙智能平台**，依托人工智能技术打造校园一体化交流服务社区。支持用户发布表白、寻物、求助、校园资讯、生活吐槽等动态内容；集成 AI 智能内容审核、AI 违规识别、智能推荐、智能分类功能。

---

## 二、模块总览

```
AI治理 (父菜单)
├── LLM大模型配置    [✅ Phase 0 已完成]
├── 提示词管理        [📋 Phase 1 待开发]
├── 知识库管理        [📋 Phase 2 待开发]
├── AI Agent 控制台   [📋 Phase 3 待开发]
└── 工具与插件管理    [📋 Phase 4 待开发]
```

---

## 三、Phase 0: LLM大模型接入 ✅ 已完成

```
已完成内容:
├── 8种主流服务商 (通义千问/Deepseek/OpenAI/Anthropic/Google/豆包/智谱AI/OpenRouter)
├── BaseURL 编辑与自动适配
├── API Key 安全管理
├── 模型测试接入与选择
├── 前端卡片式管理界面
└── 完整 RESTful API
```

---

## 四、Phase 1: 提示词管理

### 4.1 功能目标

- 支持编写系统提示词和用户提示词
- 支持提示词占位符变量 `{{variable}}`
- 支持一键 AI 生成/润色提示词
- 内置校园墙场景专属模板
- 支持提示词模板的 CRUD 和启用/禁用

### 4.2 数据库设计

```sql
-- 提示词模板表
CREATE TABLE ai_prompt_template (
  template_id     BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '模板ID',
  template_name   VARCHAR(100) NOT NULL                 COMMENT '模板名称',
  template_type   CHAR(1)      NOT NULL DEFAULT '0'     COMMENT '类型(0系统提示词 1用户提示词)',
  scene_code      VARCHAR(50)  DEFAULT NULL             COMMENT '场景代码(content_audit/recommend/classify/chat)',
  scene_name      VARCHAR(100) DEFAULT NULL             COMMENT '场景名称',
  prompt_content  TEXT         NOT NULL                 COMMENT '提示词内容',
  variables       VARCHAR(500) DEFAULT NULL             COMMENT '变量列表(JSON数组)',
  system_prompt_id BIGINT(20)  DEFAULT NULL             COMMENT '关联系统提示词ID(用户提示词时可关联)',
  status          CHAR(1)      DEFAULT '0'              COMMENT '状态(0启用 1停用)',
  is_builtin      CHAR(1)      DEFAULT '0'              COMMENT '是否内置(0否 1是)',
  sort            INT(4)       DEFAULT 0                COMMENT '显示顺序',
  remark          VARCHAR(500) DEFAULT NULL             COMMENT '备注',
  create_by       VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME                              COMMENT '创建时间',
  update_by       VARCHAR(64)  DEFAULT ''               COMMENT '更新者',
  update_time     DATETIME                              COMMENT '更新时间',
  PRIMARY KEY (template_id)
) ENGINE=InnoDB COMMENT='AI提示词模板表';

-- 提示词生成历史表
CREATE TABLE ai_prompt_generation (
  gen_id          BIGINT(20)    NOT NULL AUTO_INCREMENT  COMMENT '生成ID',
  user_input      TEXT          NOT NULL                 COMMENT '用户输入的需求描述',
  generated_prompt TEXT         NOT NULL                 COMMENT 'AI生成的提示词',
  template_type   CHAR(1)       DEFAULT NULL             COMMENT '生成类型(0系统提示词 1用户提示词)',
  llm_config_id   BIGINT(20)    DEFAULT NULL             COMMENT '使用的LLM配置ID',
  model_name      VARCHAR(100)  DEFAULT NULL             COMMENT '使用的模型名称',
  create_by       VARCHAR(64)   DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME                               COMMENT '创建时间',
  PRIMARY KEY (gen_id)
) ENGINE=InnoDB COMMENT='提示词生成历史表';
```

### 4.3 内置模板（校园墙场景）

| 场景代码 | 场景名称 | 模板类型 | 说明 |
|---------|---------|---------|------|
| content_audit | AI内容审核 | 系统 | 审核帖子内容，识别违规信息 |
| content_classify | AI智能分类 | 系统 | 将帖子分类到对应板块 |
| content_recommend | AI智能推荐 | 系统 | 根据用户偏好推荐内容 |
| comment_moderate | AI评论管理 | 系统 | 审核评论，过滤恶意言论 |
| campus_chat | 校园智能助手 | 系统 | 与用户进行校园相关对话 |
| user_report | 举报分析 | 系统 | 智能分析举报内容真实性 |

### 4.4 前端页面结构

```
views/ai/prompt/
├── index.vue              # 提示词管理主页面（卡片式）
├── components/
│   ├── PromptCard.vue     # 提示词模板卡片
│   ├── PromptEditor.vue   # 提示词编辑器（含变量占位符辅助）
│   └── PromptGenerator.vue # AI生成/润色对话框
```

### 4.5 API设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 列表 | GET | /ai/prompt/list | 提示词模板列表 |
| 详情 | GET | /ai/prompt/{id} | 获取模板详情 |
| 新增 | POST | /ai/prompt | 新增模板 |
| 修改 | PUT | /ai/prompt | 修改模板 |
| 删除 | DELETE | /ai/prompt/{ids} | 删除模板 |
| AI生成 | POST | /ai/prompt/generate | 调用LLM生成/润色提示词 |
| 预览 | POST | /ai/prompt/preview | 预览替换变量后的提示词 |
| 变更状态 | PUT | /ai/prompt/changeStatus | 启用/停用 |

### 4.6 后端文件规划

```
ruoyi-system/src/main/java/com/ruoyi/system/
├── domain/
│   ├── AiPromptTemplate.java       # 提示词模板实体
│   └── vo/
│       ├── AiPromptTemplateQuery.java  # 查询VO
│       └── PromptGenerateVO.java       # 生成请求VO
├── mapper/
│   ├── AiPromptTemplateMapper.java     # Mapper接口
│   └── AiPromptGenerationMapper.java   # 生成历史Mapper
├── service/
│   ├── IAiPromptTemplateService.java   # Service接口
│   └── impl/
│       └── AiPromptTemplateServiceImpl.java # Service实现
├── service/
│   ├── IAiPromptGenerateService.java   # AI生成服务接口
│   └── impl/
│       └── AiPromptGenerateServiceImpl.java # AI生成实现
└── controller/system/
    └── AiPromptController.java         # Controller

resources/mapper/system/
├── AiPromptTemplateMapper.xml
└── AiPromptGenerationMapper.xml
```

### 4.7 权限标识

| 权限标识 | 说明 |
|---------|------|
| ai:prompt:list | 查看提示词列表 |
| ai:prompt:add | 新增提示词 |
| ai:prompt:edit | 编辑提示词 |
| ai:prompt:remove | 删除提示词 |
| ai:prompt:generate | AI生成/润色 |

---

## 五、Phase 2: 知识库管理

### 5.1 功能目标

- 支持多知识库管理
- 支持文件上传（txt/pdf/docx/xlsx/md/图片）
- 文件内容自动解析和文本提取
- 支持向量化存储（可选集成 Embedding）
- 支持知识库检索（语义搜索）
- 供 Agent 调用进行 RAG 增强

### 5.2 数据库设计

```sql
-- 知识库表
CREATE TABLE ai_knowledge_base (
  kb_id           BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '知识库ID',
  kb_name         VARCHAR(100) NOT NULL                 COMMENT '知识库名称',
  kb_desc         VARCHAR(500) DEFAULT NULL             COMMENT '知识库描述',
  embedding_model VARCHAR(100) DEFAULT NULL             COMMENT '向量化模型',
  doc_count       INT(11)      DEFAULT 0                COMMENT '文档数量',
  chunk_count     INT(11)      DEFAULT 0                COMMENT '分块总数',
  status          CHAR(1)      DEFAULT '0'              COMMENT '状态(0正常 1停用)',
  create_by       VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME                              COMMENT '创建时间',
  update_by       VARCHAR(64)  DEFAULT ''               COMMENT '更新者',
  update_time     DATETIME                              COMMENT '更新时间',
  PRIMARY KEY (kb_id)
) ENGINE=InnoDB COMMENT='AI知识库表';

-- 知识库文档表
CREATE TABLE ai_knowledge_document (
  doc_id          BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '文档ID',
  kb_id           BIGINT(20)   NOT NULL                 COMMENT '知识库ID',
  doc_name        VARCHAR(200) NOT NULL                 COMMENT '文档名称',
  doc_type        VARCHAR(20)  DEFAULT NULL             COMMENT '文档类型(txt/pdf/docx/xlsx/md/jpg/png)',
  file_path       VARCHAR(500) DEFAULT NULL             COMMENT '文件存储路径',
  file_size       BIGINT(20)   DEFAULT 0                COMMENT '文件大小(字节)',
  content_text    LONGTEXT     DEFAULT NULL             COMMENT '提取的文本内容',
  chunk_count     INT(11)      DEFAULT 0                COMMENT '分块数量',
  status          CHAR(1)      DEFAULT '0'              COMMENT '状态(0处理中 1完成 2失败)',
  error_msg       VARCHAR(500) DEFAULT NULL             COMMENT '错误信息',
  create_by       VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME                              COMMENT '创建时间',
  PRIMARY KEY (doc_id),
  KEY idx_kb_id (kb_id)
) ENGINE=InnoDB COMMENT='AI知识库文档表';

-- 文档分块表
CREATE TABLE ai_document_chunk (
  chunk_id        BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '分块ID',
  doc_id          BIGINT(20)   NOT NULL                 COMMENT '文档ID',
  kb_id           BIGINT(20)   NOT NULL                 COMMENT '知识库ID',
  chunk_index     INT(11)      NOT NULL                 COMMENT '分块序号',
  chunk_content   TEXT         NOT NULL                 COMMENT '分块文本',
  chunk_vector    JSON         DEFAULT NULL             COMMENT '向量数据(JSON格式)',
  token_count     INT(11)      DEFAULT 0                COMMENT 'Token数量',
  PRIMARY KEY (chunk_id),
  KEY idx_doc_id (doc_id),
  KEY idx_kb_id (kb_id)
) ENGINE=InnoDB COMMENT='文档分块表';
```

### 5.3 技术方案

```
文件上传 → 文本提取 → 文本分块 → 可选向量化 → 存入数据库
                ↓
        查询时: 语义搜索 → 返回相关分块 → LLM汇总
```

**文件解析策略：**
| 文件类型 | 解析方式 |
|---------|---------|
| .txt | 直接读取 |
| .md | 直接读取 |
| .pdf | Apache PDFBox |
| .docx | Apache POI |
| .xlsx | Apache POI |
| .jpg/.png | Tesseract OCR (可选) |

### 5.4 前端页面结构

```
views/ai/knowledge/
├── index.vue                # 知识库列表（卡片式）
├── components/
│   ├── KnowledgeCard.vue    # 知识库卡片
│   ├── DocumentList.vue     # 文档列表（表格）
│   └── UploadDialog.vue     # 文件上传对话框(拖拽上传+进度)
```

### 5.5 API设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 列表 | GET | /ai/knowledge/base/list | 知识库列表 |
| 详情 | GET | /ai/knowledge/base/{id} | 知识库详情 |
| 新增 | POST | /ai/knowledge/base | 新增知识库 |
| 修改 | PUT | /ai/knowledge/base | 修改知识库 |
| 删除 | DELETE | /ai/knowledge/base/{ids} | 删除知识库 |
| 文档列表 | GET | /ai/knowledge/doc/list | 文档列表 |
| 上传文档 | POST | /ai/knowledge/doc/upload | 上传文档 |
| 删除文档 | DELETE | /ai/knowledge/doc/{ids} | 删除文档 |
| 检索 | POST | /ai/knowledge/search | 知识库检索 |

### 5.6 后端文件规划

```
ruoyi-system/src/main/java/com/ruoyi/system/
├── domain/
│   ├── AiKnowledgeBase.java
│   ├── AiKnowledgeDocument.java
│   └── AiDocumentChunk.java
├── mapper/
│   ├── AiKnowledgeBaseMapper.java
│   ├── AiKnowledgeDocumentMapper.java
│   └── AiDocumentChunkMapper.java
├── service/
│   ├── IAiKnowledgeBaseService.java
│   ├── IAiKnowledgeDocumentService.java
│   ├── IAiDocumentParserService.java      # 文档解析
│   └── impl/
│       ├── AiKnowledgeBaseServiceImpl.java
│       ├── AiKnowledgeDocumentServiceImpl.java
│       └── AiDocumentParserServiceImpl.java
└── controller/system/
    └── AiKnowledgeController.java
```

---

## 六、Phase 3: AI Agent 控制台（核心）

### 6.1 功能目标

- 对话式前端界面，与 Agent 实时交互
- 多轮对话记忆能力（会话上下文管理）
- 支持选择 LLM 配置和提示词模板
- 支持知识库挂载（RAG 增强）
- 支持工具调用结果展示
- 对话历史管理（新建/切换/删除会话）

### 6.2 数据库设计

```sql
-- 会话表
CREATE TABLE ai_conversation (
  conversation_id BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '会话ID',
  title           VARCHAR(200) DEFAULT '新对话'          COMMENT '会话标题',
  llm_config_id   BIGINT(20)   NOT NULL                 COMMENT '使用的LLM配置ID',
  prompt_template_id BIGINT(20) DEFAULT NULL            COMMENT '使用的提示词模板ID',
  kb_id           BIGINT(20)   DEFAULT NULL             COMMENT '关联知识库ID',
  memory_mode     CHAR(1)      DEFAULT '1'              COMMENT '记忆模式(0无记忆 1短期记忆 2长期记忆)',
  max_tokens      INT(11)      DEFAULT 4096             COMMENT '上下文最大Token数',
  message_count   INT(11)      DEFAULT 0                COMMENT '消息数量',
  status          CHAR(1)      DEFAULT '0'              COMMENT '状态(0进行中 1已完成)',
  create_by       VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME                              COMMENT '创建时间',
  update_by       VARCHAR(64)  DEFAULT ''               COMMENT '更新者',
  update_time     DATETIME                              COMMENT '更新时间',
  PRIMARY KEY (conversation_id),
  KEY idx_create_by (create_by)
) ENGINE=InnoDB COMMENT='AI对话会话表';

-- 会话消息表
CREATE TABLE ai_conversation_message (
  message_id      BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '消息ID',
  conversation_id BIGINT(20)   NOT NULL                 COMMENT '会话ID',
  role            VARCHAR(20)  NOT NULL                 COMMENT '角色(system/user/assistant/tool)',
  content         LONGTEXT     NOT NULL                 COMMENT '消息内容',
  token_count     INT(11)      DEFAULT 0                COMMENT 'Token数量',
  tool_calls      JSON         DEFAULT NULL             COMMENT '工具调用(JSON)',
  metadata        JSON         DEFAULT NULL             COMMENT '元数据(含知识库引用等)',
  create_time     DATETIME                              COMMENT '创建时间',
  PRIMARY KEY (message_id),
  KEY idx_conversation_id (conversation_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB COMMENT='AI对话消息表';

-- 知识库引用表（消息与知识库的关联）
CREATE TABLE ai_message_kb_ref (
  ref_id          BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '引用ID',
  message_id      BIGINT(20)   NOT NULL                 COMMENT '消息ID',
  chunk_id        BIGINT(20)   NOT NULL                 COMMENT '分块ID',
  relevance_score DECIMAL(5,4) DEFAULT 0                COMMENT '相关度评分',
  PRIMARY KEY (ref_id),
  KEY idx_message_id (message_id)
) ENGINE=InnoDB COMMENT='消息知识库引用表';
```

### 6.3 Agent 架构设计

```
┌─────────────────────────────────────────────────────┐
│                   AI Agent 控制台                     │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐ │
│  │ 会话管理  │  │ 记忆管理  │  │ 知识库检索(RAG)    │ │
│  │          │  │          │  │                   │ │
│  │·新建会话 │  │·短期记忆 │  │·语义搜索           │ │
│  │·历史列表 │  │·长期记忆 │  │·分块引用           │ │
│  │·切换删除 │  │·摘要压缩 │  │·来源标注           │ │
│  └──────────┘  └──────────┘  └───────────────────┘ │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │               LLM 调用层                      │   │
│  │                                               │   │
│  │  系统提示词 + 知识库上下文 + 对话历史           │   │
│  │  → 调用 LLM API → 解析响应                    │   │
│  │  → 处理 Function Calling → 执行工具           │   │
│  │  → 生成最终回复                                │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
│  ┌──────────────────────────────────────────────┐   │
│  │               工具执行层                      │   │
│  │                                               │   │
│  │  ·联网搜索  ·知识库查询  ·内容审核             │   │
│  │  ·日期计算  ·代码执行   ·数据查询             │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### 6.4 记忆机制设计

**短期记忆：**
- 保留最近 N 轮对话（默认10轮）
- 超过限制时，对早期对话自动摘要压缩
- Token 计数和窗口管理

**长期记忆：**
- 提取用户偏好、历史话题、关键信息
- 通过 `ai_user_memory` 表持久化存储
- 每次对话时注入相关长期记忆

```sql
-- 用户长期记忆表
CREATE TABLE ai_user_memory (
  memory_id       BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '记忆ID',
  user_id         BIGINT(20)   NOT NULL                 COMMENT '用户ID',
  memory_key      VARCHAR(100) NOT NULL                 COMMENT '记忆键(话题/偏好/关键信息)',
  memory_value    TEXT         NOT NULL                 COMMENT '记忆内容',
  importance      INT(4)       DEFAULT 1                COMMENT '重要性(1-5)',
  last_access     DATETIME                              COMMENT '最后访问时间',
  access_count    INT(11)      DEFAULT 0                COMMENT '访问次数',
  create_time     DATETIME                              COMMENT '创建时间',
  PRIMARY KEY (memory_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB COMMENT='AI用户长期记忆表';
```

### 6.5 前端页面结构

```
views/ai/agent/
├── index.vue              # Agent控制台主页面
├── components/
│   ├── ConversationSidebar.vue  # 左侧会话列表
│   ├── ChatWindow.vue           # 中间对话窗口
│   ├── AgentSettings.vue        # 右侧Agent设置面板
│   ├── MessageBubble.vue        # 消息气泡(含工具调用展示)
│   ├── ModelSelector.vue        # 模型/知识库选择器
│   └── ToolResultCard.vue       # 工具调用结果卡片
```

### 6.6 对话界面布局

```
┌──────────┬────────────────────────┬──────────────┐
│ 会话列表  │ 对话区域                  │ 设置面板      │
│          │                         │              │
│ [+新对话] │ ┌─────────────────────┐ │ 模型选择     │
│          │ │ 系统: 你好!我是...   │ │ [通义千问 ▼]│
│ 历史对话1 │ │                      │ │              │
│ 历史对话2 │ │ 用户: 帮我审核...    │ │ 提示词模板   │
│ 历史对话3 │ │                      │ │ [内容审核 ▼]│
│          │ │ AI: 经审核发现...    │ │              │
│          │ │ 📎 参考来源:         │ │ 知识库      │
│          │ │  · 校园规范 第3条    │ │ [校园规范 ▼]│
│          │ │  · 管理员守则 第8条  │ │              │
│          │ │                      │ │ 记忆模式    │
│          │ │                      │ │ ● 短期记忆  │
│          │ │                      │ │ ○ 长期记忆  │
│          │ │                      │ │              │
│          │ │ [输入消息...] [发送] │ │              │
│          │ └─────────────────────┘ │              │
└──────────┴────────────────────────┴──────────────┘
```

### 6.7 API设计

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 会话列表 | GET | /ai/agent/conversation/list | 会话列表 |
| 新建会话 | POST | /ai/agent/conversation | 新建会话 |
| 删除会话 | DELETE | /ai/agent/conversation/{id} | 删除会话 |
| 消息列表 | GET | /ai/agent/message/list/{conversationId} | 获取消息 |
| 发送消息 | POST | /ai/agent/chat | 发送消息(SSE流式) |
| 停止生成 | POST | /ai/agent/chat/stop | 停止生成 |
| 清空上下文 | POST | /ai/agent/conversation/{id}/clear | 清空对话 |
| 记忆列表 | GET | /ai/agent/memory/list | 长期记忆列表 |
| 删除记忆 | DELETE | /ai/agent/memory/{ids} | 删除记忆 |

### 6.8 后端文件规划

```
ruoyi-system/src/main/java/com/ruoyi/system/
├── domain/
│   ├── AiConversation.java
│   ├── AiConversationMessage.java
│   ├── AiUserMemory.java
│   └── AiMessageKbRef.java
├── mapper/
│   ├── AiConversationMapper.java
│   ├── AiConversationMessageMapper.java
│   ├── AiUserMemoryMapper.java
│   └── AiMessageKbRefMapper.java
├── service/
│   ├── IAiConversationService.java
│   ├── IAiAgentService.java          # Agent核心服务
│   ├── IAiMemoryService.java         # 记忆管理服务
│   └── impl/
│       ├── AiConversationServiceImpl.java
│       ├── AiAgentServiceImpl.java    # Agent核心实现
│       └── AiMemoryServiceImpl.java
└── controller/system/
    └── AiAgentController.java

-- 核心类
ruoyi-system/src/main/java/com/ruoyi/system/agent/
├── AgentEngine.java           # Agent引擎（编排核心）
├── AgentContext.java          # Agent上下文（系统提示词+历史+知识库）
├── MemoryManager.java         # 记忆管理器
├── PromptBuilder.java         # 提示词构建器
├── tool/
│   ├── ToolRegistry.java      # 工具注册中心
│   ├── AbstractTool.java      # 工具抽象类
│   └── impl/
│       └── WebSearchTool.java # 联网搜索工具
```

---

## 七、Phase 4: 工具与联网检索

### 7.1 功能目标

- 内置工具：联网搜索、知识库查询、日期计算
- 工具注册与发现机制
- Function Calling 协议支持
- 工具执行结果可视化

### 7.2 数据库设计

```sql
-- 工具注册表
CREATE TABLE ai_tool (
  tool_id         BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '工具ID',
  tool_name       VARCHAR(100) NOT NULL                 COMMENT '工具名称',
  tool_code       VARCHAR(50)  NOT NULL                 COMMENT '工具代码',
  tool_desc       VARCHAR(500) DEFAULT NULL             COMMENT '工具描述',
  function_schema JSON         NOT NULL                 COMMENT 'Function Calling Schema定义',
  handler_class   VARCHAR(200) NOT NULL                 COMMENT '处理类全限定名',
  is_builtin      CHAR(1)      DEFAULT '1'              COMMENT '是否内置(0否 1是)',
  status          CHAR(1)      DEFAULT '0'              COMMENT '状态(0启用 1停用)',
  sort            INT(4)       DEFAULT 0                COMMENT '排序',
  create_by       VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  create_time     DATETIME                              COMMENT '创建时间',
  PRIMARY KEY (tool_id),
  UNIQUE KEY uk_tool_code (tool_code)
) ENGINE=InnoDB COMMENT='AI工具注册表';
```

### 7.3 内置工具列表

| 工具代码 | 工具名称 | 说明 |
|---------|---------|------|
| web_search | 联网搜索 | 通过搜索引擎检索最新信息 |
| kb_query | 知识库查询 | 查询项目知识库相关内容 |
| content_audit | 内容审核 | 审核帖子/评论内容 |
| date_calc | 日期计算 | 计算日期差值等 |
| weather | 天气查询 | 查询指定城市天气(可选) |

### 7.4 联网搜索方案

```
方案A: 调用搜索引擎API (必应搜索API / SerpAPI)
方案B: 使用第三方搜索服务 (Tavily API)
方案C: 使用免费API (DuckDuckGo Instant Answer API)

推荐: 方案A/B 二选一，备选方案C
```

### 7.5 工具调用流程

```
用户输入 → Agent解析意图 → 判断是否需要工具
  ├── 不需要 → 直接LLM回复
  └── 需要 → Function Calling
              ├── 调用联网搜索
              ├── 调用知识库查询  
              └── 调用其他工具
              ↓
         收集工具结果 → 注入上下文 → LLM整合回复 → 返回用户
```

---

## 八、开发路线图

| 阶段 | 模块 | 预估工作量 | 依赖 |
|------|------|-----------|------|
| **Phase 1** | 提示词管理 | 3-4天 | Phase 0 (已完成) |
| **Phase 2** | 知识库管理 | 4-5天 | Phase 0 |
| **Phase 3** | AI Agent 控制台 | 5-7天 | Phase 1 + Phase 2 |
| **Phase 4** | 工具与联网检索 | 3-4天 | Phase 3 |
| **Phase 5** | 集成测试与优化 | 2-3天 | Phase 1-4 |

**总计预估：17-23 天**

---

## 九、菜单与权限规划

### 9.1 菜单结构

```
AI治理 (menu_type=M, parent_id=0)
├── LLM大模型配置    (已完成✅)
├── 提示词管理        (Phase 1)
├── 知识库管理        (Phase 2)
├── AI Agent 控制台   (Phase 3)
└── 工具管理          (Phase 4)
```

### 9.2 权限汇总

| 模块 | 权限标识前缀 |
|------|-------------|
| LLM配置 | `ai:llm:*` |
| 提示词管理 | `ai:prompt:*` |
| 知识库管理 | `ai:knowledge:*` |
| Agent控制台 | `ai:agent:*` |
| 工具管理 | `ai:tool:*` |

---

## 十、技术选型

| 功能 | 技术方案 |
|------|---------|
| LLM调用 | RestTemplate (已完成) |
| 流式响应(SSE) | Spring WebFlux / SseEmitter |
| 文档解析 | Apache PDFBox + POI |
| 向量化(可选) | 调用LLM Embedding API |
| 记忆管理 | MySQL持久化 + 内存缓存 |
| 联网搜索 | Tavily API / Bing Search API |
| 前端对话 | 自定义Vue组件 |
| Markdown渲染 | marked.js + highlight.js |

---

## 十一、验收标准

- [ ] 提示词管理支持完整 CRUD 和 AI 生成/润色
- [ ] 知识库支持文件上传、解析、检索
- [ ] Agent支持多轮对话记忆，切换会话不丢失上下文
- [ ] Agent能正确调用联网搜索工具
- [ ] Agent能基于知识库进行 RAG 增强回答
- [ ] SSE流式输出，打字机效果
- [ ] 工具调用过程和结果可追溯展示
- [ ] 对话历史可持久化和回放
- [ ] 权限控制完善
- [ ] 界面交互流畅，符合校园墙风格

---

**文档状态：** 规划中 → 等待用户审批
