package com.daou.agent.application.quant;

import java.time.LocalDate;

public record MarketPricePoint(
        LocalDate date,
        double close
) {
    public MarketPricePoint {
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (!Double.isFinite(close) || close <= 0) {
            throw new IllegalArgumentException("close must be positive finite value");
        }
    }
}
