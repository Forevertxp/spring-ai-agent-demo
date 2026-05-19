package com.hxy.agent.entity;

public enum IntentType {
    DATA_QUERY,       // 数据查询：查具体数值
    COMPARISON,       // 对比分析：对比不同维度
    TREND_ANALYSIS,   // 趋势分析：分析变化趋势
    ANOMALY_DIAG,     // 异常诊断：分析异常原因
    RECOMMENDATION,   // 建议请求：寻求改进建议
    UNKNOWN           // 无法识别
}