package com.ruoyi.system.agent.tool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.domain.AiTool;
import com.ruoyi.system.mapper.AiToolMapper;

/**
 * 工具注册中心
 *
 * 启动时从数据库加载所有启用的工具，通过反射获取 Handler 实例。
 * 为 Agent 引擎提供 tools schema 和工具执行能力。
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AiToolMapper aiToolMapper;

    /** 工具代码 -> 工具实例 */
    private final Map<String, AbstractTool> tools = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadTools();
    }

    /**
     * 重新加载工具(管理后台增删改后调用)
     */
    public void refresh() {
        tools.clear();
        loadTools();
    }

    /**
     * 获取用于 LLM Function Calling 的 tools 数组
     */
    public JSONArray getToolsForLLM() {
        JSONArray array = new JSONArray();
        for (AbstractTool tool : tools.values()) {
            try {
                JSONObject function = JSON.parseObject(tool.getFunctionSchema());
                if (function != null) {
                    JSONObject toolObj = new JSONObject();
                    toolObj.put("type", "function");
                    toolObj.put("function", function);
                    array.add(toolObj);
                }
            } catch (Exception e) {
                log.warn("解析工具 {} 的 function_schema 失败", tool.getToolCode(), e);
            }
        }
        return array;
    }

    /**
     * 根据工具名称获取工具实例
     */
    public AbstractTool getTool(String toolCode) {
        return tools.get(toolCode);
    }

    /**
     * 执行指定工具
     */
    public ToolResult execute(String toolCode, JSONObject arguments) {
        AbstractTool tool = tools.get(toolCode);
        if (tool == null) {
            return ToolResult.error("工具不存在或已停用: " + toolCode);
        }
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            log.error("工具 {} 执行异常", toolCode, e);
            return ToolResult.error("工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取已注册的工具数量
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * 是否有可用工具
     */
    public boolean hasTools() {
        return !tools.isEmpty();
    }

    // --- private ---

    private void loadTools() {
        java.util.List<AiTool> toolConfigs = aiToolMapper.selectEnabledTools();
        for (AiTool config : toolConfigs) {
            try {
                Class<?> clazz = Class.forName(config.getHandlerClass());
                AbstractTool tool = (AbstractTool) applicationContext.getBean(clazz);
                tool.setToolConfig(config);
                tools.put(config.getToolCode(), tool);
                log.info("加载工具: {} -> {}", config.getToolCode(), config.getToolName());
            } catch (Exception e) {
                log.warn("加载工具失败 [{}]: {}", config.getToolCode(), e.getMessage());
            }
        }
        log.info("工具注册中心初始化完成，共加载 {} 个工具", tools.size());
    }
}
