package com.ruoyi.system.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AiTool;
import com.ruoyi.system.domain.vo.AiToolQuery;
import com.ruoyi.system.service.IAiToolService;

/**
 * AI工具Controller
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@RestController
@RequestMapping("/ai/tool")
public class AiToolController extends BaseController {

    @Autowired
    private IAiToolService aiToolService;

    /**
     * 获取工具列表
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:list')")
    @GetMapping("/list")
    public TableDataInfo list(AiToolQuery query) {
        startPage();
        List<AiTool> list = aiToolService.selectAiToolList(query);
        return getDataTable(list);
    }

    /**
     * 获取工具详情
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:query')")
    @GetMapping("/{toolId}")
    public AjaxResult getInfo(@PathVariable("toolId") Long toolId) {
        return success(aiToolService.selectAiToolById(toolId));
    }

    /**
     * 新增工具
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:add')")
    @Log(title = "AI工具管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AiTool tool) {
        tool.setCreateBy(getUsername());
        return toAjax(aiToolService.insertAiTool(tool));
    }

    /**
     * 修改工具
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:edit')")
    @Log(title = "AI工具管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody AiTool tool) {
        tool.setUpdateBy(getUsername());
        return toAjax(aiToolService.updateAiTool(tool));
    }

    /**
     * 删除工具
     */
    @PreAuthorize("@ss.hasPermi('ai:tool:remove')")
    @Log(title = "AI工具管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{toolIds}")
    public AjaxResult remove(@PathVariable Long[] toolIds) {
        return toAjax(aiToolService.deleteAiToolByIds(toolIds));
    }
}
