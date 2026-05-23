package com.ruoyi.system.agent.tool.impl;

import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.agent.tool.AbstractTool;
import com.ruoyi.system.agent.tool.ToolResult;

/**
 * 天气查询工具 — 查询指定城市天气信息
 *
 * 当前返回模拟数据（需要付费API Key）。
 * 未来可接入和风天气或OpenWeatherMap API。
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Component
public class WeatherTool extends AbstractTool {

    @Override
    public ToolResult execute(JSONObject arguments) throws Exception {
        String city = arguments.getString("city");
        if (city == null || city.isBlank()) {
            return ToolResult.error("城市名称不能为空");
        }

        // 当前为模拟数据，未来接入真实天气API
        String result = String.format(
            "【%s 天气信息】（模拟数据）\n\n" +
            "当前温度: 22°C\n" +
            "天气状况: 多云转晴\n" +
            "湿度: 65%%\n" +
            "风力: 东北风 3级\n" +
            "温馨提示: 天气数据来自模拟，实际使用时请接入天气API（如和风天气、OpenWeatherMap）",
            city
        );

        return ToolResult.ok(result);
    }
}
