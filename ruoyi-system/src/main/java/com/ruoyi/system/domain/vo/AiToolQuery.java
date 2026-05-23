package com.ruoyi.system.domain.vo;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI工具查询对象
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public class AiToolQuery extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 工具名称 */
    private String toolName;

    /** 工具代码 */
    private String toolCode;

    /** 状态 */
    private String status;

    /** 是否内置 */
    private String isBuiltin;

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIsBuiltin() {
        return isBuiltin;
    }

    public void setIsBuiltin(String isBuiltin) {
        this.isBuiltin = isBuiltin;
    }
}
