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
public class CustomerFlowData {
    private String storeCode;
    private String storeName;
    private String timeRange;
    private int totalFlow;
    private double conversionRate;
    private int newCustomers;
    private int repeatCustomers;
    private double newCustomerRatio;
    private boolean success;
    private String errorMessage;

    public static CustomerFlowData error(String message) {
        return CustomerFlowData.builder()
                .success(false)
                .errorMessage(message)
                .build();
    }
}