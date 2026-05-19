package com.hxy.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxy.agent.entity.IntentResult;
import com.hxy.agent.entity.QueryEntities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class IntentRouter {

    private final ObjectMapper objectMapper;

    public IntentRouter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public IntentResult analyzeIntent(String userQuery) {
        log.info("分析意图: {}", userQuery);

        String query = userQuery.toLowerCase();

        IntentResult result = IntentResult.builder()
                .entities(QueryEntities.builder().build())
                .confidence(0.8)
                .build();

        if (containsAny(query, Arrays.asList("对比", "比较", "差异", "哪个更好"))) {
            result.setIntent(com.hxy.agent.entity.IntentType.COMPARISON);
        } else if (containsAny(query, Arrays.asList("趋势", "变化", "走势", "增长", "下降"))) {
            result.setIntent(com.hxy.agent.entity.IntentType.TREND_ANALYSIS);
        } else if (containsAny(query, Arrays.asList("为什么", "原因", "怎么回事", "异常"))) {
            result.setIntent(com.hxy.agent.entity.IntentType.ANOMALY_DIAG);
        } else if (containsAny(query, Arrays.asList("建议", "如何提升", "怎么办", "改进"))) {
            result.setIntent(com.hxy.agent.entity.IntentType.RECOMMENDATION);
        } else if (containsAny(query, Arrays.asList("查询", "多少", "数据", "财务", "客流", "库存", "营收", "利润"))) {
            result.setIntent(com.hxy.agent.entity.IntentType.DATA_QUERY);
        } else {
            result.setIntent(com.hxy.agent.entity.IntentType.DATA_QUERY);
            result.setConfidence(0.6);
        }

        log.info("意图识别结果: intent={}, confidence={}", result.getIntent(), result.getConfidence());
        return result;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}