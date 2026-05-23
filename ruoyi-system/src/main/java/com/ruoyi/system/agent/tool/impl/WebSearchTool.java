package com.ruoyi.system.agent.tool.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.agent.tool.AbstractTool;
import com.ruoyi.system.agent.tool.ToolResult;

/**
 * 联网搜索工具 — 基于 Tavily Search API
 *
 * Tavily API 文档: https://docs.tavily.com/
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Component
public class WebSearchTool extends AbstractTool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);

    private static final String TAVILY_URL = "https://api.tavily.com/search";

    @Autowired
    private RestTemplate restTemplate;

    /** Tavily API Key，在 application.yml 中配置: ruoyi.tavily.api-key */
    @Value("${ruoyi.tavily.api-key:}")
    private String tavilyApiKey;

    @Override
    public ToolResult execute(JSONObject arguments) throws Exception {
        String query = arguments.getString("query");
        if (query == null || query.isBlank()) {
            return ToolResult.error("搜索关键词不能为空");
        }

        int maxResults = arguments.getIntValue("max_results", 5);
        if (maxResults < 1) maxResults = 5;
        if (maxResults > 10) maxResults = 10;

        if (tavilyApiKey == null || tavilyApiKey.isBlank()) {
            return ToolResult.error("Tavily API Key 未配置，请在 application.yml 中设置 ruoyi.tavily.api-key");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("api_key", tavilyApiKey);
            body.put("query", query);
            body.put("max_results", maxResults);
            body.put("search_depth", "basic");
            body.put("include_answer", true);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(body), headers);

            String response = restTemplate.postForObject(TAVILY_URL, entity, String.class);
            if (response == null) {
                return ToolResult.error("Tavily API 返回空响应");
            }

            JSONObject result = JSON.parseObject(response);

            // 构建结果文本
            StringBuilder sb = new StringBuilder();
            String answer = result.getString("answer");
            if (answer != null && !answer.isBlank()) {
                sb.append("【AI摘要】").append(answer).append("\n\n");
            }

            JSONArray results = result.getJSONArray("results");
            if (results != null && !results.isEmpty()) {
                sb.append("【搜索结果】\n");
                for (int i = 0; i < results.size(); i++) {
                    JSONObject item = results.getJSONObject(i);
                    sb.append(i + 1).append(". ");
                    sb.append(item.getString("title")).append("\n");
                    sb.append("   ").append(item.getString("content")).append("\n");
                    String url = item.getString("url");
                    if (url != null) sb.append("   🔗 ").append(url).append("\n");
                    sb.append("\n");
                }
            }

            if (sb.length() == 0) {
                return ToolResult.ok("未找到与 \"" + query + "\" 相关的搜索结果。");
            }

            // 截断过长内容
            String content = sb.toString();
            if (content.length() > 3000) {
                content = content.substring(0, 3000) + "\n...(结果已截断)";
            }

            Map<String, Object> meta = new HashMap<>();
            meta.put("query", query);
            meta.put("resultCount", results != null ? results.size() : 0);

            return ToolResult.ok(content, meta);

        } catch (Exception e) {
            log.error("Tavily 搜索异常: query={}", query, e);
            return ToolResult.error("联网搜索失败: " + e.getMessage());
        }
    }
}
