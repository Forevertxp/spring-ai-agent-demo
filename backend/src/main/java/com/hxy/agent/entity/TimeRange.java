package com.hxy.agent.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeRange {
    private String start;
    private String end;
    private TimeUnit unit;

    public static TimeRange unknown() {
        return new TimeRange(null, null, null);
    }
}