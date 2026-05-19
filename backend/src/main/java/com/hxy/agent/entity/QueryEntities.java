package com.hxy.agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryEntities {
    private List<String> storeNames;
    private TimeRange timeRange;
    private List<String> metrics;
    private String dimension;
    private String aggregation;
}