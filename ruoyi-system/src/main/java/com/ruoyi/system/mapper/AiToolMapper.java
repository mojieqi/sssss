package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AiTool;
import com.ruoyi.system.domain.vo.AiToolQuery;

/**
 * AI工具Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-23
 */
public interface AiToolMapper {

    /**
     * 查询工具
     *
     * @param toolId 工具主键
     * @return AI工具
     */
    public AiTool selectAiToolById(Long toolId);

    /**
     * 查询工具列表
     *
     * @param query 查询条件
     * @return AI工具集合
     */
    public List<AiTool> selectAiToolList(AiToolQuery query);

    /**
     * 查询所有启用的工具
     *
     * @return AI工具集合
     */
    public List<AiTool> selectEnabledTools();

    /**
     * 新增工具
     *
     * @param tool AI工具
     * @return 结果
     */
    public int insertAiTool(AiTool tool);

    /**
     * 修改工具
     *
     * @param tool AI工具
     * @return 结果
     */
    public int updateAiTool(AiTool tool);

    /**
     * 删除工具
     *
     * @param toolId 工具主键
     * @return 结果
     */
    public int deleteAiToolById(Long toolId);

    /**
     * 批量删除工具
     *
     * @param toolIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAiToolByIds(Long[] toolIds);
}
