package com.ruoyi.system.agent.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.domain.AiTool;

/**
 * 工具抽象基类
 *
 * 所有内置工具需继承此类并标注 @Component，
 * 由 ToolRegistry 通过 Spring ApplicationContext 获取实例。
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public abstract class AbstractTool {

    /** 数据库中的工具配置 */
    protected AiTool toolConfig;

    /**
     * 执行工具
     *
     * @param arguments LLM传入的Function Calling参数(已解析为JSONObject)
     * @return 工具执行结果
     * @throws Exception 执行异常
     */
    public abstract ToolResult execute(JSONObject arguments) throws Exception;

    /**
     * 获取工具代码(与数据库 tool_code 一致)
     */
    public String getToolCode() {
        return toolConfig != null ? toolConfig.getToolCode() : null;
    }

    /**
     * 获取 Function Calling Schema(JSON字符串)
     */
    public String getFunctionSchema() {
        return toolConfig != null ? toolConfig.getFunctionSchema() : null;
    }

    public AiTool getToolConfig() {
        return toolConfig;
    }

    public void setToolConfig(AiTool toolConfig) {
        this.toolConfig = toolConfig;
    }
}
