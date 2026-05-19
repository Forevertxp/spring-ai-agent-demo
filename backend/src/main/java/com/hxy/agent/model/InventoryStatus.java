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
public class InventoryStatus {
    private String storeCode;
    private String storeName;
    private double turnoverRate;
    private int totalItems;
    private int lowStockItems;
    private int outOfStockItems;
    private List<StockAlert> alerts;
    private boolean success;
    private String errorMessage;

    public static InventoryStatus error(String message) {
        return InventoryStatus.builder()
                .success(false)
                .errorMessage(message)
                .build();
    }
}