package com.ruoyi.system.agent.tool.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.agent.tool.AbstractTool;
import com.ruoyi.system.agent.tool.ToolResult;
import com.ruoyi.system.service.IEmbeddingService;

/**
 * 知识库查询工具 — 查询项目知识库内容
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Component
public class KbQueryTool extends AbstractTool {

    @Autowired
    private IEmbeddingService embeddingService;

    @Override
    public ToolResult execute(JSONObject arguments) throws Exception {
        String query = arguments.getString("query");
        if (query == null || query.isBlank()) {
            return ToolResult.error("查询关键词不能为空");
        }

        int topK = arguments.getIntValue("top_k", 5);
        if (topK < 1) topK = 5;
        if (topK > 10) topK = 10;

        try {
            List<IEmbeddingService.ChunkSearchResult> results =
                    embeddingService.searchGlobal(query, topK);

            if (results == null || results.isEmpty()) {
                return ToolResult.ok("在知识库中未找到与 \"" + query + "\" 相关的内容。");
            }

            StringBuilder sb = new StringBuilder("【知识库检索结果】\n\n");
            for (int i = 0; i < results.size(); i++) {
                IEmbeddingService.ChunkSearchResult r = results.get(i);
                sb.append("--- 结果 ").append(i + 1);
                if (r.getSimilarityScore() != null) {
                    sb.append(" (相关度: ").append(String.format("%.2f", r.getSimilarityScore())).append(")");
                }
                sb.append(" ---\n");
                sb.append(r.getChunkContent()).append("\n\n");
            }

            String content = sb.toString();
            if (content.length() > 3000) {
                content = content.substring(0, 3000) + "\n...(结果已截断)";
            }

            return ToolResult.ok(content);

        } catch (Exception e) {
            return ToolResult.error("知识库查询失败: " + e.getMessage());
        }
    }
}
