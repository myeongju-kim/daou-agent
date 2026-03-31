package com.daou.agent.application.quant;

import java.util.List;

public record MarketPrediction(
        String displayName,
        String symbol,
        String assetType,
        String horizon,
        String direction,
        double currentPrice,
        double predictedPrice,
        double rangeLow,
        double rangeHigh,
        double confidence,
        List<String> evidence,
        String riskSummary,
        String dataSource,
        String asOfDate,
        int dataPoints
) {
    public MarketPrediction {
        displayName = blankToDash(displayName);
        symbol = blankToDash(symbol);
        assetType = blankToDash(assetType);
        horizon = blankToDash(horizon);
        direction = blankToDash(direction);
        riskSummary = blankToDash(riskSummary);
        dataSource = blankToDash(dataSource);
        asOfDate = blankToDash(asOfDate);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    private static String blankToDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
