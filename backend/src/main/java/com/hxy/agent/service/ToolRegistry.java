package com.hxy.agent.service;

import com.hxy.agent.model.ToolCallRequest;
import com.hxy.agent.model.ToolCallResult;
import com.hxy.agent.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new HashMap<>();

    private final FinancialDataTool financialDataTool;
    private final InventoryTool inventoryTool;
    private final TimeParserTool timeParserTool;
    private final StoreCodeResolverTool storeCodeResolverTool;

    public ToolRegistry(FinancialDataTool financialDataTool,
                        InventoryTool inventoryTool,
                        TimeParserTool timeParserTool,
                        StoreCodeResolverTool storeCodeResolverTool) {
        this.financialDataTool = financialDataTool;
        this.inventoryTool = inventoryTool;
        this.timeParserTool = timeParserTool;
        this.storeCodeResolverTool = storeCodeResolverTool;

        registerTools();
    }

    private void registerTools() {
        // 注册财务数据工具
        registerTool("get_financial_data",
                "查询门店财务数据，包括营收、利润、成本、利润率",
                Map.of(
                    "storeCode", "门店编码(BJ001/SH001/GZ001/SZ001)",
                    "startDate", "开始日期(yyyy-MM-dd)",
                    "endDate", "结束日期(yyyy-MM-dd)",
                    "metric", "指标类型(revenue/profit/cost/margin)，可选"
                ),
                params -> financialDataTool.getFinancialData(
                    (String) params.get("storeCode"),
                    (String) params.get("startDate"),
                    (String) params.get("endDate"),
                    (String) params.getOrDefault("metric", "all")
                ));

        // 注册多门店财务数据工具
        registerTool("get_multi_store_financial",
                "查询多个门店的财务数据用于对比分析",
                Map.of(
                    "storeCodes", "门店编码列表，逗号分隔(如BJ001,SH001)",
                    "startDate", "开始日期(yyyy-MM-dd)",
                    "endDate", "结束日期(yyyy-MM-dd)"
                ),
                params -> financialDataTool.getMultiStoreFinancial(
                    (String) params.get("storeCodes"),
                    (String) params.get("startDate"),
                    (String) params.get("endDate"),
                    "all"
                ));

        // 注册财务趋势工具
        registerTool("get_financial_trend",
                "查询门店财务数据趋势，返回每日明细用于趋势分析",
                Map.of(
                    "storeCode", "门店编码",
                    "startDate", "开始日期(yyyy-MM-dd)",
                    "endDate", "结束日期(yyyy-MM-dd)"
                ),
                params -> financialDataTool.getFinancialTrend(
                    (String) params.get("storeCode"),
                    (String) params.get("startDate"),
                    (String) params.get("endDate"),
                    "all"
                ));

        // 注册库存数据工具
        registerTool("get_inventory_status",
                "查询门店库存状态，包括周转率、库存数量、缺货预警",
                Map.of(
                    "storeCode", "门店编码"
                ),
                params -> inventoryTool.getInventoryStatus(
                    (String) params.get("storeCode")
                ));

        // 注册时间解析工具
        registerTool("parse_time",
                "解析相对时间表述为具体日期范围，如'上个月'、'最近三个月'、'Q1'",
                Map.of(
                    "timeExpression", "时间表述(如上个月、最近三个月、Q1、第一季度)"
                ),
                params -> timeParserTool.parseTime(
                    (String) params.get("timeExpression")
                ));

        // 注册门店编码解析工具
        registerTool("resolve_store_code",
                "将门店名称转换为门店编码",
                Map.of(
                    "storeName", "门店名称(如北京、上海、北京门店)"
                ),
                params -> storeCodeResolverTool.resolveStoreCode(
                    (String) params.get("storeName")
                ));

        log.info("已注册{}个工具: {}", tools.size(), tools.keySet());
    }

    public void registerTool(String name, String description,
                             Map<String, String> parameters,
                             Function<Map<String, Object>, Object> executor) {
        tools.put(name, new ToolDefinition(name, description, parameters, executor));
        log.debug("注册工具: {} - {}", name, description);
    }

    public ToolCallResult executeTool(ToolCallRequest request) {
        log.info("执行工具调用: tool={}, params={}", request.getToolName(), request.getParameters());

        ToolDefinition tool = tools.get(request.getToolName());
        if (tool == null) {
            log.warn("工具不存在: {}", request.getToolName());
            return ToolCallResult.builder()
                    .callId(request.getCallId())
                    .toolName(request.getToolName())
                    .success(false)
                    .errorMessage("工具不存在: " + request.getToolName())
                    .build();
        }

        try {
            Object result = tool.getExecutor().apply(request.getParameters());
            log.info("工具执行成功: tool={}, resultType={}", request.getToolName(),
                    result != null ? result.getClass().getSimpleName() : "null");

            return ToolCallResult.builder()
                    .callId(request.getCallId())
                    .toolName(request.getToolName())
                    .result(result)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("工具执行失败: tool={}, error={}", request.getToolName(), e.getMessage());
            return ToolCallResult.builder()
                    .callId(request.getCallId())
                    .toolName(request.getToolName())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    public String getToolDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用工具列表:\n\n");

        for (ToolDefinition tool : tools.values()) {
            sb.append("### ").append(tool.getName()).append("\n");
            sb.append("描述: ").append(tool.getDescription()).append("\n");
            sb.append("参数:\n");
            for (Map.Entry<String, String> param : tool.getParameters().entrySet()) {
                sb.append("- ").append(param.getKey()).append(": ").append(param.getValue()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public Map<String, ToolDefinition> getAllTools() {
        return tools;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ToolDefinition {
        private String name;
        private String description;
        private Map<String, String> parameters;
        private Function<Map<String, Object>, Object> executor;
    }
}