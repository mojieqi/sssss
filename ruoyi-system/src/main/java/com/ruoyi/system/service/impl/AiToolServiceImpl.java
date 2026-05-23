package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.AiTool;
import com.ruoyi.system.domain.vo.AiToolQuery;
import com.ruoyi.system.mapper.AiToolMapper;
import com.ruoyi.system.service.IAiToolService;

/**
 * AI工具Service实现
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Service
public class AiToolServiceImpl implements IAiToolService {

    @Autowired
    private AiToolMapper aiToolMapper;

    @Override
    public AiTool selectAiToolById(Long toolId) {
        return aiToolMapper.selectAiToolById(toolId);
    }

    @Override
    public List<AiTool> selectAiToolList(AiToolQuery query) {
        return aiToolMapper.selectAiToolList(query);
    }

    @Override
    public List<AiTool> selectEnabledTools() {
        return aiToolMapper.selectEnabledTools();
    }

    @Override
    public int insertAiTool(AiTool tool) {
        return aiToolMapper.insertAiTool(tool);
    }

    @Override
    public int updateAiTool(AiTool tool) {
        return aiToolMapper.updateAiTool(tool);
    }

    @Override
    public int deleteAiToolById(Long toolId) {
        return aiToolMapper.deleteAiToolById(toolId);
    }

    @Override
    public int deleteAiToolByIds(Long[] toolIds) {
        return aiToolMapper.deleteAiToolByIds(toolIds);
    }
}
