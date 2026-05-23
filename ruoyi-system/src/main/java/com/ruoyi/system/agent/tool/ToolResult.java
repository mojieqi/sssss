package com.ruoyi.system.agent.tool;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行结果
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public class ToolResult {

    /** 是否成功 */
    private boolean success;

    /** 结果内容(文本) */
    private String content;

    /** 附加元数据 */
    private Map<String, Object> metadata = new HashMap<>();

    public static ToolResult ok(String content) {
        ToolResult r = new ToolResult();
        r.success = true;
        r.content = content;
        return r;
    }

    public static ToolResult ok(String content, Map<String, Object> metadata) {
        ToolResult r = ok(content);
        r.metadata = metadata;
        return r;
    }

    public static ToolResult error(String errorMsg) {
        ToolResult r = new ToolResult();
        r.success = false;
        r.content = errorMsg;
        return r;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
