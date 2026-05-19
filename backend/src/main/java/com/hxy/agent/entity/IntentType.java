package com.hxy.agent.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum IntentType {
    DATA_QUERY,       // 数据查询：查具体数值
    COMPARISON,       // 对比分析：对比不同维度
    TREND_ANALYSIS,   // 趋势分析：分析变化趋势
    ANOMALY_DIAG,     // 异常诊断：分析异常原因
    RECOMMENDATION,   // 建议请求：寻求改进建议
    UNKNOWN;          // 无法识别

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static IntentType fromValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        String upperValue = value.toUpperCase().replace("-", "_");
        for (IntentType type : values()) {
            if (type.name().equals(upperValue)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}