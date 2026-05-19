package com.hxy.agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {
    private IntentType intent;
    private QueryEntities entities;
    private double confidence;

    public static IntentResult unknown() {
        return IntentResult.builder()
                .intent(IntentType.UNKNOWN)
                .confidence(0.0)
                .build();
    }
}