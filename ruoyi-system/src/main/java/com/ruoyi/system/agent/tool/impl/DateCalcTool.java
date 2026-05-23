package com.ruoyi.system.agent.tool.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.agent.tool.AbstractTool;
import com.ruoyi.system.agent.tool.ToolResult;

/**
 * 日期计算工具 — 计算日期差或日期加减
 *
 * @author ruoyi
 * @date 2026-05-23
 */
@Component
public class DateCalcTool extends AbstractTool {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public ToolResult execute(JSONObject arguments) throws Exception {
        String date1Str = arguments.getString("date1");
        String operation = arguments.getString("operation");

        if (date1Str == null || date1Str.isBlank()) {
            return ToolResult.error("日期1不能为空");
        }
        if (operation == null || operation.isBlank()) {
            return ToolResult.error("操作类型不能为空");
        }

        try {
            Date date1 = DATE_FMT.parse(date1Str);

            switch (operation) {
                case "diff": {
                    String date2Str = arguments.getString("date2");
                    if (date2Str == null || date2Str.isBlank()) {
                        return ToolResult.error("日期差计算需要提供 date2");
                    }
                    Date date2 = DATE_FMT.parse(date2Str);
                    long diffMillis = Math.abs(date2.getTime() - date1.getTime());
                    long diffDays = diffMillis / (1000 * 60 * 60 * 24);
                    return ToolResult.ok(String.format(
                        "%s 与 %s 相差 %d 天", date1Str, date2Str, diffDays));
                }

                case "add": {
                    int amount = arguments.getIntValue("amount", 0);
                    String unit = arguments.getString("unit", "day");
                    Date result = addDate(date1, amount, unit);
                    return ToolResult.ok(String.format(
                        "%s + %d%s = %s", date1Str, amount, unitDisplay(unit), DATE_FMT.format(result)));
                }

                case "subtract": {
                    int amount = arguments.getIntValue("amount", 0);
                    String unit = arguments.getString("unit", "day");
                    Date result = addDate(date1, -amount, unit);
                    return ToolResult.ok(String.format(
                        "%s - %d%s = %s", date1Str, amount, unitDisplay(unit), DATE_FMT.format(result)));
                }

                default:
                    return ToolResult.error("不支持的操作类型: " + operation + "，支持: diff, add, subtract");
            }
        } catch (java.text.ParseException e) {
            return ToolResult.error("日期格式错误，请使用 yyyy-MM-dd 格式，如 2026-05-23");
        }
    }

    private Date addDate(Date date, int amount, String unit) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        switch (unit) {
            case "year":
                cal.add(Calendar.YEAR, amount);
                break;
            case "month":
                cal.add(Calendar.MONTH, amount);
                break;
            default: // day
                cal.add(Calendar.DAY_OF_MONTH, amount);
                break;
        }
        return cal.getTime();
    }

    private String unitDisplay(String unit) {
        switch (unit) {
            case "year": return "年";
            case "month": return "月";
            default: return "天";
        }
    }
}
