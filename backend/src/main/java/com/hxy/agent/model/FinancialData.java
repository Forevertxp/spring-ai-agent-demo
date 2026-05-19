package com.hxy.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialData {
    private String storeCode;
    private String storeName;
    private String timeRange;
    private double revenue;
    private double profit;
    private double cost;
    private double profitMargin;
    private boolean success;
    private String errorMessage;

    public static FinancialData error(String message) {
        return FinancialData.builder()
                .success(false)
                .errorMessage(message)
                .build();
    }
}