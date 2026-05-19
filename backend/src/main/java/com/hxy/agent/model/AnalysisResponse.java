package com.hxy.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponse {
    private String analysis;
    private List<ChartConfig> charts;
    private Map<String, Object> data;
    private List<String> suggestions;

    public static AnalysisResponse from(AnalysisResult result) {
        return AnalysisResponse.builder()
                .analysis(result.getAnalysis())
                .charts(result.getCharts())
                .data(result.getRawData())
                .suggestions(result.getSuggestions())
                .build();
    }
}