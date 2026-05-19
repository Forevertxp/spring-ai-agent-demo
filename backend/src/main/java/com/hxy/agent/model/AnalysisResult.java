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
public class AnalysisResult {
    private String analysis;
    private List<ChartConfig> charts;
    private Map<String, Object> rawData;
    private List<String> suggestions;
}