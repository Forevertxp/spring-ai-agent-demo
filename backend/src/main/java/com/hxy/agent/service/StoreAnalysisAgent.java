package com.hxy.agent.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResultCallback;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxy.agent.entity.IntentResult;
import com.hxy.agent.entity.IntentType;
import com.hxy.agent.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class StoreAnalysisAgent {

    @Value("${spring.ai.alibaba.api-key}")
    private String apiKey;

    @Value("${spring.ai.alibaba.chat.options.model:qwen-plus}")
    private String model;

    private final Generation generation;
    private final IntentRouter intentRouter;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    private final Map<String, List<Message>> sessionMessages = new HashMap<>();
    private final Map<String, List<ToolCallResult>> sessionToolResults = new HashMap<>();

    public StoreAnalysisAgent(Generation generation,
                              IntentRouter intentRouter,
                              ToolRegistry toolRegistry) {
        this.generation = generation;
        this.intentRouter = intentRouter;
        this.toolRegistry = toolRegistry;
        this.objectMapper = new ObjectMapper();
    }

    public AnalysisResult analyze(String sessionId, String userQuery) {
        log.info("分析请求: sessionId={}, query={}", sessionId, userQuery);

        // 1. 意图识别
        IntentResult intent = intentRouter.analyzeIntent(userQuery);
        log.info("意图识别结果: intent={}, confidence={}", intent.getIntent(), intent.getConfidence());

        // 2. 让AI决定需要调用哪些工具
        List<ToolCallRequest> toolCalls = determineToolCalls(sessionId, userQuery, intent);
        log.info("AI决定调用{}个工具: {}", toolCalls.size(),
                toolCalls.stream().map(ToolCallRequest::getToolName).toList());

        // 3. 执行工具调用
        List<ToolCallResult> toolResults = new ArrayList<>();
        for (ToolCallRequest request : toolCalls) {
            ToolCallResult result = toolRegistry.executeTool(request);
            toolResults.add(result);
        }
        sessionToolResults.put(sessionId, toolResults);
        log.info("工具执行完成，成功{}个，失败{}个",
                toolResults.stream().filter(ToolCallResult::isSuccess).count(),
                toolResults.stream().filter(r -> !r.isSuccess()).count());

        // 4. 将工具结果给AI进行最终分析
        String analysis = generateFinalAnalysis(sessionId, userQuery, toolResults);

        // 5. 提取图表配置
        List<ChartConfig> charts = extractChartsFromToolResults(toolResults);

        // 6. 构建原始数据
        Map<String, Object> rawData = buildRawDataFromToolResults(toolResults);

        return AnalysisResult.builder()
                .analysis(analysis)
                .charts(charts)
                .rawData(rawData)
                .suggestions(Arrays.asList(
                        "查看更详细的数据分析",
                        "对比其他门店",
                        "获取改进建议"
                ))
                .build();
    }

    public Flux<String> analyzeStream(String sessionId, String userQuery) {
        log.info("流式分析请求: sessionId={}, query={}", sessionId, userQuery);

        // 1. 意图识别
        IntentResult intent = intentRouter.analyzeIntent(userQuery);
        log.info("意图识别: {}", intent.getIntent());

        // 2. 决定工具调用
        List<ToolCallRequest> toolCalls = determineToolCalls(sessionId, userQuery, intent);
        log.info("工具调用: {}个", toolCalls.size());

        // 3. 执行工具
        List<ToolCallResult> toolResults = new ArrayList<>();
        for (ToolCallRequest request : toolCalls) {
            ToolCallResult result = toolRegistry.executeTool(request);
            toolResults.add(result);
        }

        // 保存工具结果到session
        sessionToolResults.put(sessionId, toolResults);
        log.info("工具执行完成: 成功{}个", toolResults.stream().filter(ToolCallResult::isSuccess).count());

        // 4. 获取完整分析结果
        String fullAnalysis = generateFinalAnalysis(sessionId, userQuery, toolResults);

        // 5. 将完整结果按段落分块返回（模拟流式效果）
        return splitIntoChunks(fullAnalysis);
    }

    private Flux<String> splitIntoChunks(String text) {
        if (text == null || text.isEmpty()) {
            return Flux.just("暂无分析结果");
        }

        // 按行分割，逐行返回
        String[] lines = text.split("\n");
        return Flux.fromArray(lines)
                .map(line -> line + "\n")
                .delayElements(java.time.Duration.ofMillis(50)); // 添加小延迟模拟流式效果
    }

    private List<ToolCallRequest> determineToolCalls(String sessionId, String userQuery, IntentResult intent) {
        // 构建工具决策提示
        String decisionPrompt = buildToolDecisionPrompt(userQuery, intent);

        // 调用AI决定工具
        String aiResponse = callDashScopeForToolDecision(decisionPrompt);
        log.info("AI工具决策响应: {}", aiResponse);

        // 解析工具调用请求
        return parseToolCallsFromResponse(aiResponse, userQuery, intent);
    }

    private String buildToolDecisionPrompt(String userQuery, IntentResult intent) {
        return """
                用户问题: %s

                已识别意图类型: %s

                %s

                请分析用户问题，决定需要调用哪些工具来获取数据。

                输出格式要求：
                请以JSON数组格式输出需要调用的工具，每个工具调用包含：
                {
                    "tool": "工具名称",
                    "parameters": {
                        "参数名": "参数值"
                    },
                    "reason": "调用原因"
                }

                时间解析规则：
                - 上个月: 2026-04-01 到 2026-04-30
                - 本月: 2026-05-01 到 2026-05-16
                - 最近三个月: 2026-02-16 到 2026-05-16
                - Q1/第一季度: 2026-01-01 到 2026-03-31

                门店编码：
                - 北京: BJ001
                - 上海: SH001
                - 广州: GZ001
                - 深圳: SZ001

                请只输出JSON数组，不要其他内容。
                """.formatted(userQuery, intent.getIntent(), toolRegistry.getToolDescriptions());
    }

    private String callDashScopeForToolDecision(String prompt) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role("system").content("""
                    你是数据分析智能体的工具决策模块。
                    你的任务是分析用户问题，决定需要调用哪些数据查询工具。
                    请严格按照JSON格式输出，不要添加任何额外文字。
                    """).build());
            messages.add(Message.builder().role("user").content(prompt).build());

            GenerationParam param = GenerationParam.builder()
                    .model(model)
                    .apiKey(apiKey)
                    .messages(messages)
                    .temperature(0.1f)  // 降低温度以提高决策准确性
                    .build();

            GenerationResult result = generation.call(param);

            if (result != null && result.getOutput() != null &&
                result.getOutput().getChoices() != null &&
                !result.getOutput().getChoices().isEmpty()) {
                return result.getOutput().getChoices().get(0).getMessage().getContent();
            }

            return "[]";

        } catch (Exception e) {
            log.error("工具决策调用失败: {}", e.getMessage());
            return "[]";
        }
    }

    private List<ToolCallRequest> parseToolCallsFromResponse(String response, String userQuery, IntentResult intent) {
        List<ToolCallRequest> requests = new ArrayList<>();

        // 尝试解析JSON
        try {
            String cleanJson = extractJsonArray(response);
            if (cleanJson != null && !cleanJson.isEmpty()) {
                JsonNode array = objectMapper.readTree(cleanJson);
                if (array.isArray()) {
                    for (JsonNode item : array) {
                        String toolName = item.has("tool") ? item.get("tool").asText() : null;
                        if (toolName == null) continue;

                        Map<String, Object> params = new HashMap<>();
                        if (item.has("parameters") && item.get("parameters").isObject()) {
                            JsonNode paramsNode = item.get("parameters");
                            paramsNode.fields().forEachRemaining(entry ->
                                params.put(entry.getKey(), parseJsonValue(entry.getValue()))
                            );
                        }

                        requests.add(ToolCallRequest.builder()
                                .toolName(toolName)
                                .parameters(params)
                                .callId(UUID.randomUUID().toString())
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("JSON解析失败，使用规则引擎: {}", e.getMessage());
        }

        // 如果JSON解析失败，使用规则引擎决定工具
        if (requests.isEmpty()) {
            requests = decideToolsByRules(userQuery, intent);
        }

        return requests;
    }

    private Object parseJsonValue(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.asBoolean();
        return node.toString();
    }

    private String extractJsonArray(String text) {
        // 提取JSON数组
        Pattern pattern = Pattern.compile("\\[\\s*\\{.*\\}\\s*\\]");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }

        // 尝试从代码块提取
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.lastIndexOf("```");
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }

        if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.lastIndexOf("```");
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }

        return null;
    }

    private List<ToolCallRequest> decideToolsByRules(String userQuery, IntentResult intent) {
        List<ToolCallRequest> requests = new ArrayList<>();

        // 解析门店
        String storeCode = parseStoreCode(userQuery);

        // 解析时间
        com.hxy.agent.entity.TimeRange timeRange = parseTimeRange(userQuery);
        String startDate = timeRange.getStart() != null ? timeRange.getStart() : "2026-04-01";
        String endDate = timeRange.getEnd() != null ? timeRange.getEnd() : "2026-04-30";

        switch (intent.getIntent()) {
            case DATA_QUERY:
                requests.add(ToolCallRequest.builder()
                        .toolName("get_financial_data")
                        .parameters(Map.of(
                                "storeCode", storeCode,
                                "startDate", startDate,
                                "endDate", endDate,
                                "metric", "all"
                        ))
                        .callId(UUID.randomUUID().toString())
                        .build());
                break;

            case COMPARISON:
                // 多门店对比
                List<String> stores = parseStores(userQuery);
                String storeCodes = stores.isEmpty() ? "BJ001,SH001" :
                        stores.stream()
                                .map(s -> toolRegistry.executeTool(ToolCallRequest.builder()
                                        .toolName("resolve_store_code")
                                        .parameters(Map.of("storeName", s))
                                        .callId("resolve-" + s)
                                        .build()).getResult().toString().split("\\(")[0])
                                .reduce((a, b) -> a + "," + b)
                                .orElse("BJ001,SH001");

                requests.add(ToolCallRequest.builder()
                        .toolName("get_multi_store_financial")
                        .parameters(Map.of(
                                "storeCodes", storeCodes,
                                "startDate", startDate,
                                "endDate", endDate
                        ))
                        .callId(UUID.randomUUID().toString())
                        .build());
                break;

            case TREND_ANALYSIS:
                requests.add(ToolCallRequest.builder()
                        .toolName("get_financial_trend")
                        .parameters(Map.of(
                                "storeCode", storeCode,
                                "startDate", startDate,
                                "endDate", endDate
                        ))
                        .callId(UUID.randomUUID().toString())
                        .build());
                break;

            case ANOMALY_DIAG:
                // 综合查询
                requests.add(ToolCallRequest.builder()
                        .toolName("get_financial_data")
                        .parameters(Map.of(
                                "storeCode", storeCode,
                                "startDate", startDate,
                                "endDate", endDate,
                                "metric", "all"
                        ))
                        .callId(UUID.randomUUID().toString())
                        .build());
                requests.add(ToolCallRequest.builder()
                        .toolName("get_inventory_status")
                        .parameters(Map.of("storeCode", storeCode))
                        .callId(UUID.randomUUID().toString())
                        .build());
                break;

            case RECOMMENDATION:
                requests.add(ToolCallRequest.builder()
                        .toolName("get_financial_data")
                        .parameters(Map.of(
                                "storeCode", storeCode,
                                "startDate", startDate,
                                "endDate", endDate,
                                "metric", "all"
                        ))
                        .callId(UUID.randomUUID().toString())
                        .build());
                requests.add(ToolCallRequest.builder()
                        .toolName("get_inventory_status")
                        .parameters(Map.of("storeCode", storeCode))
                        .callId(UUID.randomUUID().toString())
                        .build());
                break;

            default:
                requests.add(ToolCallRequest.builder()
                        .toolName("get_financial_data")
                        .parameters(Map.of(
                                "storeCode", storeCode,
                                "startDate", startDate,
                                "endDate", endDate,
                                "metric", "all"
                        ))
                        .callId(UUID.randomUUID().toString())
                        .build());
        }

        return requests;
    }

    private String generateFinalAnalysis(String sessionId, String userQuery, List<ToolCallResult> toolResults) {
        // 构建包含工具结果的提示
        String analysisPrompt = buildAnalysisPrompt(userQuery, toolResults);

        // 调用AI生成最终分析
        String analysis = callDashScopeForAnalysis(sessionId, analysisPrompt);

        // 如果AI调用失败，使用fallback分析
        if (analysis == null || analysis.isEmpty() || analysis.contains("错误") || analysis.contains("失败")) {
            log.warn("AI分析失败或返回空，使用fallback分析");
            analysis = buildFallbackAnalysis(toolResults);
        }

        return analysis;
    }

    private String buildAnalysisPrompt(String userQuery, List<ToolCallResult> toolResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户问题: ").append(userQuery).append("\n\n");
        prompt.append("工具调用结果:\n\n");

        for (ToolCallResult result : toolResults) {
            prompt.append("### 工具: ").append(result.getToolName()).append("\n");
            if (result.isSuccess()) {
                prompt.append("结果:\n");
                prompt.append(formatToolResult(result.getResult()));
            } else {
                prompt.append("错误: ").append(result.getErrorMessage()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("""
                请基于以上工具返回的数据，进行专业分析，给出见解和建议。

                ## 分析要求
                1. 使用Markdown格式输出
                2. 包含数据概览表格
                3. 给出分析结论
                4. 提供改进建议
                5. 对比行业基准数据(月营收50-80万，利润率15-20%)

                """);

        return prompt.toString();
    }

    private String formatToolResult(Object result) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            return result.toString();
        }
    }

    private String callDashScopeForAnalysis(String sessionId, String prompt) {
        try {
            List<Message> messages = sessionMessages.computeIfAbsent(sessionId, k -> new ArrayList<>());

            if (messages.isEmpty()) {
                messages.add(Message.builder().role("system").content("""
                        你是门店经营数据分析智能体。

                        ## 分析原则
                        - 基于数据说话，不主观臆断
                        - 对比要有参照（行业均值、历史数据）
                        - 发现异常要深入分析原因
                        - 建议要具体可行，有优先级

                        ## 行业基准数据
                        - 月营收基准：一线城市50-80万
                        - 利润率基准：15-20%为正常水平
                        - 客流转化率基准：20-30%
                        - 库存周转率基准：4-6次/年

                        请使用Markdown格式输出分析报告。
                        """).build());
            }

            messages.add(Message.builder().role("user").content(prompt).build());

            GenerationParam param = GenerationParam.builder()
                    .model(model)
                    .apiKey(apiKey)
                    .messages(messages)
                    .temperature(0.7f)
                    .build();

            GenerationResult result = generation.call(param);

            if (result != null && result.getOutput() != null &&
                result.getOutput().getChoices() != null &&
                !result.getOutput().getChoices().isEmpty()) {

                String response = result.getOutput().getChoices().get(0).getMessage().getContent();
                messages.add(Message.builder().role("assistant").content(response).build());

                return response;
            }

            return buildFallbackAnalysis(sessionToolResults.getOrDefault(sessionId, new ArrayList<>()));

        } catch (Exception e) {
            log.error("分析调用失败: {}", e.getMessage());
            return buildFallbackAnalysis(sessionToolResults.getOrDefault(sessionId, new ArrayList<>()));
        }
    }

    private Flux<String> streamFinalAnalysis(String sessionId, String userQuery, List<ToolCallResult> toolResults) {
        String analysisPrompt = buildAnalysisPrompt(userQuery, toolResults);

        return Flux.create(emitter -> {
            try {
                List<Message> messages = sessionMessages.computeIfAbsent(sessionId, k -> new ArrayList<>());

                if (messages.isEmpty()) {
                    messages.add(Message.builder().role("system").content("""
                            你是门店经营数据分析智能体。
                            基于数据进行分析，使用Markdown格式输出。
                            """).build());
                }

                messages.add(Message.builder().role("user").content(analysisPrompt).build());

                GenerationParam param = GenerationParam.builder()
                        .model(model)
                        .apiKey(apiKey)
                        .messages(messages)
                        .temperature(0.7f)
                        .incrementalOutput(true)
                        .build();

                StringBuilder fullResponse = new StringBuilder();

                // 使用streamCall并直接迭代
                generation.streamCall(param).forEach(result -> {
                    try {
                        if (result != null && result.getOutput() != null) {
                            var output = result.getOutput();
                            if (output.getChoices() != null && !output.getChoices().isEmpty()) {
                                var choice = output.getChoices().get(0);
                                if (choice.getMessage() != null) {
                                    String content = choice.getMessage().getContent();
                                    if (content != null && !content.isEmpty()) {
                                        fullResponse.append(content);
                                        emitter.next(content);
                                        log.debug("流式输出: {}", content);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("处理流式结果异常: {}", e.getMessage());
                    }
                });

                // 完成后将完整响应保存到session
                if (fullResponse.length() > 0) {
                    messages.add(Message.builder().role("assistant").content(fullResponse.toString()).build());
                    log.info("流式响应完成，总长度: {}", fullResponse.length());
                } else {
                    log.warn("流式响应为空");
                }
                emitter.complete();

            } catch (Exception e) {
                log.error("流式调用失败: {}", e.getMessage(), e);
                emitter.error(e);
            }
        });
    }

    private String buildFallbackAnalysis(List<ToolCallResult> results) {
        log.info("buildFallbackAnalysis: results.size={}", results != null ? results.size() : 0);

        if (results == null || results.isEmpty()) {
            return "暂无数据可分析";
        }

        StringBuilder analysis = new StringBuilder();
        analysis.append("## 数据分析报告\n\n");

        for (ToolCallResult result : results) {
            log.info("处理工具结果: tool={}, success={}, resultType={}",
                    result.getToolName(), result.isSuccess(),
                    result.getResult() != null ? result.getResult().getClass().getSimpleName() : "null");

            if (!result.isSuccess()) {
                analysis.append("### 工具调用失败\n");
                analysis.append("工具: ").append(result.getToolName()).append("\n");
                analysis.append("错误: ").append(result.getErrorMessage()).append("\n\n");
                continue;
            }

            Object data = result.getResult();
            if (data == null) {
                log.warn("工具结果为null: {}", result.getToolName());
                continue;
            }

            // 处理单个财务数据
            if (data instanceof FinancialData) {
                FinancialData f = (FinancialData) data;
                analysis.append("### 财务数据\n\n");
                analysis.append("| 指标 | 数值 |\n|------|------|\n");
                analysis.append(String.format("| 门店 | %s |\n", f.getStoreName()));
                analysis.append(String.format("| 营收 | %.2f万元 |\n", f.getRevenue() / 10000));
                analysis.append(String.format("| 利润 | %.2f万元 |\n", f.getProfit() / 10000));
                analysis.append(String.format("| 成本 | %.2f万元 |\n", f.getCost() / 10000));
                analysis.append(String.format("| 利润率 | %.2f%% |\n\n", f.getProfitMargin()));

                // 添加分析结论
                analysis.append("### 分析结论\n\n");
                if (f.getProfitMargin() >= 18) {
                    analysis.append("门店利润率表现优秀，高于行业平均水平(15-20%)。\n\n");
                } else if (f.getProfitMargin() >= 15) {
                    analysis.append("门店利润率处于正常水平，符合行业基准。\n\n");
                } else {
                    analysis.append("门店利润率低于行业基准，需要关注成本控制和运营效率。\n\n");
                }

                analysis.append("### 改进建议\n\n");
                analysis.append("1. 持续监控营收和利润变化\n");
                analysis.append("2. 优化成本结构，提升利润率\n");
                analysis.append("3. 加强客流运营，提升转化率\n");
            }

            // 处理财务数据列表（对比或趋势）
            if (data instanceof List) {
                List<?> list = (List<?>) data;
                log.info("处理列表数据: size={}", list.size());
                if (!list.isEmpty() && list.get(0) instanceof FinancialData) {
                    @SuppressWarnings("unchecked")
                    List<FinancialData> dataList = (List<FinancialData>) list;

                    analysis.append("### 对比数据\n\n");
                    analysis.append("| 门店 | 营收 | 利润 | 利润率 |\n|------|------|------|--------|\n");
                    for (FinancialData f : dataList) {
                        analysis.append(String.format("| %s | %.2f万 | %.2f万 | %.2f%% |\n",
                                f.getStoreName(), f.getRevenue() / 10000, f.getProfit() / 10000, f.getProfitMargin()));
                    }
                    analysis.append("\n");

                    // 对比分析结论
                    if (dataList.size() >= 2) {
                        FinancialData first = dataList.get(0);
                        FinancialData second = dataList.get(1);
                        analysis.append("### 对比分析\n\n");
                        analysis.append(String.format("%s营收%.2f万，%s营收%.2f万，差距%.2f万。\n\n",
                                first.getStoreName(), first.getRevenue() / 10000,
                                second.getStoreName(), second.getRevenue() / 10000,
                                Math.abs(first.getRevenue() - second.getRevenue()) / 10000));
                    }
                }
            }

            // 处理库存数据
            if (data instanceof InventoryStatus) {
                InventoryStatus inv = (InventoryStatus) data;
                analysis.append("### 库存状态\n\n");
                analysis.append("| 指标 | 数值 |\n|------|------|\n");
                analysis.append(String.format("| 总商品数 | %d |\n", inv.getTotalItems()));
                analysis.append(String.format("| 周转率 | %.2f次/年 |\n", inv.getTurnoverRate()));
                analysis.append(String.format("| 低库存商品 | %d |\n", inv.getLowStockItems()));
                analysis.append("\n");
            }
        }

        log.info("fallback分析完成，长度: {}", analysis.length());
        return analysis.toString();
    }

    private List<ChartConfig> extractChartsFromToolResults(List<ToolCallResult> results) {
        List<ChartConfig> charts = new ArrayList<>();

        for (ToolCallResult result : results) {
            if (!result.isSuccess()) continue;

            if (result.getResult() instanceof List) {
                List<?> list = (List<?>) result.getResult();
                if (!list.isEmpty() && list.get(0) instanceof FinancialData) {
                    @SuppressWarnings("unchecked")
                    List<FinancialData> dataList = (List<FinancialData>) list;

                    // 判断是趋势数据还是对比数据
                    if (result.getToolName().equals("get_financial_trend")) {
                        // 趋势图
                        List<String> dates = new ArrayList<>();
                        List<Double> revenues = new ArrayList<>();
                        for (FinancialData f : dataList) {
                            dates.add(f.getTimeRange());
                            revenues.add(f.getRevenue() / 10000);
                        }
                        charts.add(ChartConfig.builder()
                                .type("line")
                                .title("营收趋势")
                                .xAxis(dates)
                                .yAxis(List.of(Map.of("name", "营收(万元)")))
                                .series(List.of(Map.of("name", "营收", "type", "line", "data", revenues, "smooth", true)))
                                .build());
                    } else if (result.getToolName().equals("get_multi_store_financial")) {
                        // 对比图
                        List<String> names = new ArrayList<>();
                        List<Double> revenues = new ArrayList<>();
                        for (FinancialData f : dataList) {
                            names.add(f.getStoreName());
                            revenues.add(f.getRevenue() / 10000);
                        }
                        charts.add(ChartConfig.builder()
                                .type("bar")
                                .title("门店对比")
                                .xAxis(names)
                                .yAxis(List.of(Map.of("name", "营收(万元)")))
                                .series(List.of(Map.of("type", "bar", "data", revenues)))
                                .build());
                    }
                }
            }
        }

        return charts;
    }

    private Map<String, Object> buildRawDataFromToolResults(List<ToolCallResult> results) {
        Map<String, Object> data = new HashMap<>();

        for (ToolCallResult result : results) {
            if (result.isSuccess()) {
                data.put(result.getToolName(), result.getResult());
            }
        }

        return data;
    }

    private String parseStoreCode(String query) {
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("北京")) return "BJ001";
        if (lowerQuery.contains("上海")) return "SH001";
        if (lowerQuery.contains("广州")) return "GZ001";
        if (lowerQuery.contains("深圳")) return "SZ001";

        return "BJ001";  // 默认北京
    }

    private List<String> parseStores(String query) {
        List<String> stores = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("北京")) stores.add("北京");
        if (lowerQuery.contains("上海")) stores.add("上海");
        if (lowerQuery.contains("广州")) stores.add("广州");
        if (lowerQuery.contains("深圳")) stores.add("深圳");

        return stores;
    }

    private com.hxy.agent.entity.TimeRange parseTimeRange(String query) {
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("上个月") || lowerQuery.contains("上月")) {
            return new com.hxy.agent.entity.TimeRange("2026-04-01", "2026-04-30",
                    com.hxy.agent.entity.TimeUnit.MONTH);
        }
        if (lowerQuery.contains("本月")) {
            return new com.hxy.agent.entity.TimeRange("2026-05-01", "2026-05-16",
                    com.hxy.agent.entity.TimeUnit.MONTH);
        }
        if (lowerQuery.contains("最近") || lowerQuery.contains("近")) {
            Pattern pattern = Pattern.compile("(最近|近)(\\d+)个?月");
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                int months = Integer.parseInt(matcher.group(2));
                return new com.hxy.agent.entity.TimeRange(
                        "2026-" + String.format("%02d", 5 - months) + "-16",
                        "2026-05-16",
                        com.hxy.agent.entity.TimeUnit.MONTH);
            }
        }
        if (lowerQuery.contains("q1") || lowerQuery.contains("第一季度")) {
            return new com.hxy.agent.entity.TimeRange("2026-01-01", "2026-03-31",
                    com.hxy.agent.entity.TimeUnit.QUARTER);
        }
        if (lowerQuery.contains("q2") || lowerQuery.contains("第二季度")) {
            return new com.hxy.agent.entity.TimeRange("2026-04-01", "2026-06-30",
                    com.hxy.agent.entity.TimeUnit.QUARTER);
        }

        return new com.hxy.agent.entity.TimeRange("2026-04-01", "2026-04-30",
                com.hxy.agent.entity.TimeUnit.MONTH);
    }
}