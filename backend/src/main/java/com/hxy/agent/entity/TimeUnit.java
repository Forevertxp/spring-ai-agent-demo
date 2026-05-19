package com.hxy.agent.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TimeUnit {
    DAY,
    WEEK,
    MONTH,
    QUARTER,
    YEAR;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static TimeUnit fromValue(String value) {
        if (value == null) {
            return null;
        }
        String upperValue = value.toUpperCase();
        for (TimeUnit unit : values()) {
            if (unit.name().equals(upperValue)) {
                return unit;
            }
        }
        return null;
    }
}