-- ----------------------------
-- AI工具注册表 & 内置工具数据
-- Phase 4: 工具与联网检索
-- ----------------------------

DROP TABLE IF EXISTS `ai_tool`;

CREATE TABLE `ai_tool` (
  `tool_id`         BIGINT(20)   NOT NULL AUTO_INCREMENT  COMMENT '工具ID',
  `tool_name`       VARCHAR(100) NOT NULL                 COMMENT '工具名称',
  `tool_code`       VARCHAR(50)  NOT NULL                 COMMENT '工具代码',
  `tool_desc`       VARCHAR(500) DEFAULT NULL             COMMENT '工具描述',
  `function_schema` JSON         NOT NULL                 COMMENT 'Function Calling Schema定义',
  `handler_class`   VARCHAR(200) NOT NULL                 COMMENT '处理类全限定名',
  `is_builtin`      CHAR(1)      DEFAULT '1'              COMMENT '是否内置(0否 1是)',
  `status`          CHAR(1)      DEFAULT '0'              COMMENT '状态(0启用 1停用)',
  `sort`            INT(4)       DEFAULT 0                COMMENT '排序',
  `create_by`       VARCHAR(64)  DEFAULT ''               COMMENT '创建者',
  `create_time`     DATETIME                              COMMENT '创建时间',
  `update_by`       VARCHAR(64)  DEFAULT ''               COMMENT '更新者',
  `update_time`     DATETIME                              COMMENT '更新时间',
  PRIMARY KEY (`tool_id`),
  UNIQUE KEY `uk_tool_code` (`tool_code`)
) ENGINE=InnoDB AUTO_INCREMENT=100 COMMENT='AI工具注册表';

-- ----------------------------
-- 内置工具初始化数据
-- ----------------------------

-- 1. 联网搜索 (Tavily)
INSERT INTO `ai_tool` (`tool_id`, `tool_name`, `tool_code`, `tool_desc`, `function_schema`, `handler_class`, `is_builtin`, `status`, `sort`, `create_by`, `create_time`)
VALUES (100, '联网搜索', 'web_search', '通过Tavily搜索引擎检索互联网最新信息，获取实时数据', 
'{"name":"web_search","description":"通过搜索引擎检索最新信息，获取互联网上的实时数据","parameters":{"type":"object","properties":{"query":{"type":"string","description":"搜索关键词或问题"},"max_results":{"type":"integer","description":"最大返回结果数，默认为5","default":5}},"required":["query"]}}',
'com.ruoyi.system.agent.tool.impl.WebSearchTool', '1', '0', 1, 'admin', sysdate());

-- 2. 知识库查询
INSERT INTO `ai_tool` (`tool_id`, `tool_name`, `tool_code`, `tool_desc`, `function_schema`, `handler_class`, `is_builtin`, `status`, `sort`, `create_by`, `create_time`)
VALUES (101, '知识库查询', 'kb_query', '查询校园知识库中的相关内容，包括校园规范、管理制度、常见问题等',
'{"name":"kb_query","description":"查询校园知识库中的相关内容，包括校园规范、管理制度、常见问题等","parameters":{"type":"object","properties":{"query":{"type":"string","description":"查询关键词或问题"},"top_k":{"type":"integer","description":"返回最相关的条数，默认为5","default":5}},"required":["query"]}}',
'com.ruoyi.system.agent.tool.impl.KbQueryTool', '1', '0', 2, 'admin', sysdate());

-- 3. 内容审核
INSERT INTO `ai_tool` (`tool_id`, `tool_name`, `tool_code`, `tool_desc`, `function_schema`, `handler_class`, `is_builtin`, `status`, `sort`, `create_by`, `create_time`)
VALUES (102, '内容审核', 'content_audit', '审核用户发布的帖子或评论内容，检测是否包含违规信息',
'{"name":"content_audit","description":"审核用户发布的帖子或评论内容，检测是否包含违规信息","parameters":{"type":"object","properties":{"content":{"type":"string","description":"待审核的文本内容"}},"required":["content"]}}',
'com.ruoyi.system.agent.tool.impl.ContentAuditTool', '1', '0', 3, 'admin', sysdate());

-- 4. 日期计算
INSERT INTO `ai_tool` (`tool_id`, `tool_name`, `tool_code`, `tool_desc`, `function_schema`, `handler_class`, `is_builtin`, `status`, `sort`, `create_by`, `create_time`)
VALUES (103, '日期计算', 'date_calc', '计算两个日期之间的差值，或对指定日期进行加减运算',
'{"name":"date_calc","description":"计算两个日期之间的差值，或对指定日期进行加减运算","parameters":{"type":"object","properties":{"date1":{"type":"string","description":"第一个日期，格式为yyyy-MM-dd"},"date2":{"type":"string","description":"第二个日期，格式为yyyy-MM-dd，仅diff操作需要"},"operation":{"type":"string","description":"操作类型","enum":["diff","add","subtract"]},"amount":{"type":"integer","description":"加减的天数，仅add/subtract时需要"},"unit":{"type":"string","description":"加减的单位","enum":["day","month","year"],"default":"day"}},"required":["date1","operation"]}}',
'com.ruoyi.system.agent.tool.impl.DateCalcTool', '1', '0', 4, 'admin', sysdate());

-- 5. 天气查询(可选)
INSERT INTO `ai_tool` (`tool_id`, `tool_name`, `tool_code`, `tool_desc`, `function_schema`, `handler_class`, `is_builtin`, `status`, `sort`, `create_by`, `create_time`)
VALUES (104, '天气查询', 'weather', '查询指定城市的实时天气信息',
'{"name":"weather","description":"查询指定城市的实时天气信息","parameters":{"type":"object","properties":{"city":{"type":"string","description":"城市名称"}},"required":["city"]}}',
'com.ruoyi.system.agent.tool.impl.WeatherTool', '1', '0', 5, 'admin', sysdate());
