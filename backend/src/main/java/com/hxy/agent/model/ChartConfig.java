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
public class ChartConfig {
    private String type;
    private String title;
    private List<String> xAxis;
    private List<Map<String, Object>> yAxis;
    private List<Map<String, Object>> series;
}