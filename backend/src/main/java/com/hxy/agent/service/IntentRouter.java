package com.hxy.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxy.agent.entity.IntentResult;
import com.hxy.agent.entity.IntentType;
import com.hxy.agent.entity.QueryEntities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class IntentRouter {

    private final ObjectMapper objectMapper;
    private final DashScopeService dashScopeService;

    private static final String INTENT_SYSTEM_PROMPT = """
            你是一个意图识别专家。请分析用户的问题，识别用户意图并提取关键实体。

            意图类型说明：
            - DATA_QUERY: 数据查询，用户想查具体数值（如"北京店本月营收是多少"）
            - COMPARISON: 对比分析，用户想对比不同维度（如"对比北京和上海店的营收"）
            - TREND_ANALYSIS: 趋势分析，用户想看变化趋势（如"最近三个月客流趋势"）
            - ANOMALY_DIAG: 异常诊断，用户想知道异常原因（如"为什么本月利润下降了"）
            - RECOMMENDATION: 建议请求，用户想获得改进建议（如"如何提升客流"）
            - UNKNOWN: 无法识别的意图

            请以JSON格式返回结果，格式如下：
            {"intent":"意图类型","confidence":0.0-1.0,"entities":{"storeNames":["门店名称"],"timeRange":{"start":"开始时间","end":"结束时间","unit":"DAY/MONTH/YEAR"},"metrics":["指标名称"],"dimension":"维度","aggregation":"聚合方式"}}

            注意：
            1. 只返回JSON，不要有其他内容
            2. confidence表示置信度，0-1之间
            3. 如果无法提取某个实体，该字段可以为null或空数组
            4. storeNames应提取中文门店名称
            5. metrics可能包括：营收、利润、客流、库存等
            """;

    public IntentRouter(ObjectMapper objectMapper, DashScopeService dashScopeService) {
        this.objectMapper = objectMapper;
        this.dashScopeService = dashScopeService;
    }

    public IntentResult analyzeIntent(String userQuery) {
        log.info("分析意图: {}", userQuery);

        try {
            String sessionId = "intent-" + UUID.randomUUID().toString().substring(0, 8);
            String response = dashScopeService.chat(sessionId, INTENT_SYSTEM_PROMPT, userQuery);
            log.info("LLM原始响应: {}", response);

            String jsonContent = extractJson(response);
            log.info("提取的JSON: {}", jsonContent);

            IntentResult result = parseIntentResult(jsonContent);
            log.info("意图识别结果: intent={}, confidence={}", result.getIntent(), result.getConfidence());
            return result;

        } catch (Exception e) {
            log.error("LLM意图识别失败，使用关键词兜底", e);
            return fallbackIntentRecognition(userQuery);
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private IntentResult parseIntentResult(String json) {
        try {
            return objectMapper.readValue(json, IntentResult.class);
        } catch (Exception e) {
            log.warn("解析LLM返回JSON失败: {}", e.getMessage());
            return IntentResult.unknown();
        }
    }

    private IntentResult fallbackIntentRecognition(String userQuery) {
        String query = userQuery.toLowerCase();

        IntentResult result = IntentResult.builder()
                .entities(QueryEntities.builder().build())
                .confidence(0.6)
                .build();

        if (query.contains("对比") || query.contains("比较") || query.contains("差异") || query.contains("哪个更好")) {
            result.setIntent(IntentType.COMPARISON);
        } else if (query.contains("趋势") || query.contains("变化") || query.contains("走势") || query.contains("增长") || query.contains("下降")) {
            result.setIntent(IntentType.TREND_ANALYSIS);
        } else if (query.contains("为什么") || query.contains("原因") || query.contains("怎么回事") || query.contains("异常")) {
            result.setIntent(IntentType.ANOMALY_DIAG);
        } else if (query.contains("建议") || query.contains("如何提升") || query.contains("怎么办") || query.contains("改进")) {
            result.setIntent(IntentType.RECOMMENDATION);
        } else {
            result.setIntent(IntentType.DATA_QUERY);
            result.setConfidence(0.4);
        }

        return result;
    }
}