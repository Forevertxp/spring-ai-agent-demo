package com.hxy.agent.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AnalysisTemplateLibrary {

    @Getter
    private final Map<String, AnalysisTemplate> templates;

    public AnalysisTemplateLibrary() {
        templates = new HashMap<>();
        initTemplates();
    }

    private void initTemplates() {
        templates.put("financial_health", AnalysisTemplate.builder()
                .name("门店财务健康度评估")
                .sections(Arrays.asList(
                        "营收分析：对比历史数据和同类门店",
                        "利润分析：毛利率变化和成本构成",
                        "现金流分析：资金周转效率",
                        "风险提示：异常指标和潜在问题",
                        "改进建议：基于数据的行动方案"
                ))
                .metricsToQuery(Arrays.asList("revenue", "profit", "cost", "margin"))
                .comparisons(Arrays.asList("last_period", "same_store_avg", "industry_avg"))
                .build());

        templates.put("customer_flow", AnalysisTemplate.builder()
                .name("门店客流分析")
                .sections(Arrays.asList(
                        "客流总量：时段分布和峰值分析",
                        "转化分析：客流到成交的转化率",
                        "客户结构：新老客户占比",
                        "异常诊断：客流波动原因",
                        "优化建议：提升客流的策略"
                ))
                .metricsToQuery(Arrays.asList("total_flow", "hourly_flow", "conversion_rate"))
                .build());

        templates.put("comprehensive", AnalysisTemplate.builder()
                .name("门店综合运营分析")
                .sections(Arrays.asList(
                        "财务表现：营收、利润、成本",
                        "客流情况：客流量、转化率",
                        "库存状态：周转率、缺货情况",
                        "综合评分：基于KPI的健康度评分",
                        "优先行动：最需要关注的问题和建议"
                ))
                .metricsToQuery(Arrays.asList("revenue", "profit", "flow", "inventory"))
                .build());
    }

    public AnalysisTemplate getTemplate(String templateName) {
        return templates.get(templateName);
    }

    public String getTemplatePrompt(String templateName) {
        AnalysisTemplate template = templates.get(templateName);
        if (template == null) return "";

        return """
                请按照以下结构进行分析：
                %s

                需要查询的指标：%s
                需要对比的维度：%s

                请确保每个部分都有具体数据支撑，并给出分析结论。
                """.formatted(
                template.getSections().stream()
                        .map(s -> "- " + s)
                        .reduce("", (a, b) -> a + "\n" + b),
                template.getMetricsToQuery().toString(),
                template.getComparisons() != null ? template.getComparisons().toString() : "无"
        );
    }

    @Getter
    public static class AnalysisTemplate {
        private String name;
        private List<String> sections;
        private List<String> metricsToQuery;
        private List<String> comparisons;

        public static AnalysisTemplate builder() {
            return new AnalysisTemplate();
        }

        public AnalysisTemplate name(String name) {
            this.name = name;
            return this;
        }

        public AnalysisTemplate sections(List<String> sections) {
            this.sections = sections;
            return this;
        }

        public AnalysisTemplate metricsToQuery(List<String> metricsToQuery) {
            this.metricsToQuery = metricsToQuery;
            return this;
        }

        public AnalysisTemplate comparisons(List<String> comparisons) {
            this.comparisons = comparisons;
            return this;
        }

        public AnalysisTemplate build() {
            return this;
        }
    }
}