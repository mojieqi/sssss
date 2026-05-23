package com.ruoyi.system.agent.tool.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.agent.tool.AbstractTool;
import com.ruoyi.system.agent.tool.ToolResult;

/**
 * 内容审核工具 — 检测文本中的违规内容
 *
 * 基于规则 + 敏感词匹配，适用于校园墙场景的帖子/评论审核。
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Component
public class ContentAuditTool extends AbstractTool {

    /** 敏感词列表(校园场景) */
    private static final String[] SENSITIVE_WORDS = {
        "暴力", "色情", "赌博", "毒品", "枪支", "诈骗", "传销",
        "侮辱", "诽谤", "人身攻击", "歧视", "骚扰",
        "作弊", "代考", "替考", "买卖答案", "论文代写",
        "校园贷", "裸贷", "高利贷",
        "自杀", "自残", "割腕"
    };

    /** 手机号正则 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /** QQ号正则 */
    private static final Pattern QQ_PATTERN = Pattern.compile("[1-9]\\d{4,10}");

    @Override
    public ToolResult execute(JSONObject arguments) throws Exception {
        String content = arguments.getString("content");
        if (content == null || content.isBlank()) {
            return ToolResult.error("审核内容不能为空");
        }

        StringBuilder result = new StringBuilder();
        result.append("【内容审核报告】\n\n");
        result.append("审核内容长度: ").append(content.length()).append(" 字符\n");

        int riskScore = 0;
        int issues = 0;

        // 1. 敏感词检测
        StringBuilder matchedWords = new StringBuilder();
        String lowerContent = content.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lowerContent.contains(word.toLowerCase())) {
                if (matchedWords.length() > 0) matchedWords.append(", ");
                matchedWords.append(word);
                riskScore += 20;
                issues++;
            }
        }

        // 2. 手机号检测
        Matcher phoneMatcher = PHONE_PATTERN.matcher(content);
        if (phoneMatcher.find()) {
            issues++;
            riskScore += 10;
        }

        // 3. 链接检测
        if (content.contains("http://") || content.contains("https://")) {
            issues++;
            riskScore += 5;
        }

        // 4. 生成结论
        result.append("发现问题: ").append(issues).append(" 处\n");
        result.append("风险评分: ").append(Math.min(riskScore, 100)).append("/100\n\n");

        if (matchedWords.length() > 0) {
            result.append("匹配敏感词: ").append(matchedWords).append("\n");
        }

        if (issues == 0) {
            result.append("\n结论: ✅ 内容通过审核，未发现违规信息。");
        } else if (riskScore < 30) {
            result.append("\n结论: ⚠️ 内容存在轻微风险，建议人工复核。");
        } else if (riskScore < 60) {
            result.append("\n结论: ⚠️ 内容存在中等风险，需要人工审核。");
        } else {
            result.append("\n结论: 🚫 内容存在高风险，建议拒绝发布。");
        }

        return ToolResult.ok(result.toString());
    }
}
