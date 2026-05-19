package com.hxy.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlert {
    private String productName;
    private int currentStock;
    private int minStock;
    private String alertType;
}