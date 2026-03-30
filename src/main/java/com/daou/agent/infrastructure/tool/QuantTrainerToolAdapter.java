package com.daou.agent.infrastructure.tool;

import com.daou.agent.application.quant.MarketPrediction;
import com.daou.agent.application.quant.QuantTrainerService;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QuantTrainerToolAdapter implements ToolAdapter {

    private final QuantTrainerService quantTrainerService;

    public QuantTrainerToolAdapter(QuantTrainerService quantTrainerService) {
        this.quantTrainerService = quantTrainerService;
    }

    @Override
    public boolean supports(String toolName) {
        return "quant.predict_market".equals(toolName);
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String target = stringArgument(request, "target", "");
        String symbol = stringArgument(request, "symbol", "");
        String assetType = stringArgument(request, "assetType", "");
        String horizon = stringArgument(request, "horizon", "day");
        List<Double> recentPrices = numberListArgument(request, "recentPrices");
        if (recentPrices.isEmpty()) {
            recentPrices = numberListArgument(request, "prices");
        }

        try {
            MarketPrediction prediction = quantTrainerService.predict(target, symbol, assetType, horizon, recentPrices);
            Map<String, Object> range = Map.of(
                    "low", prediction.rangeLow(),
                    "high", prediction.rangeHigh()
            );

            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("target", prediction.displayName());
            raw.put("symbol", prediction.symbol());
            raw.put("assetType", prediction.assetType());
            raw.put("horizon", prediction.horizon());
            raw.put("direction", prediction.direction());
            raw.put("currentPrice", prediction.currentPrice());
            raw.put("predictedPrice", prediction.predictedPrice());
            raw.put("range", range);
            raw.put("confidence", prediction.confidence());
            raw.put("confidencePercent", Math.round(prediction.confidence() * 100));
            raw.put("evidence", prediction.evidence());
            raw.put("riskSummary", prediction.riskSummary());
            raw.put("dataSource", prediction.dataSource());
            raw.put("asOfDate", prediction.asOfDate());
            raw.put("dataPoints", prediction.dataPoints());

            String message = "%s %s 전망은 %s이며 예상 범위는 %.2f~%.2f 입니다. (신뢰도 %d%%)".formatted(
                    prediction.displayName(),
                    prediction.horizon(),
                    prediction.direction(),
                    prediction.rangeLow(),
                    prediction.rangeHigh(),
                    Math.round(prediction.confidence() * 100)
            );
            return ToolCallResult.success(request.toolName(), message, raw);
        } catch (IllegalArgumentException e) {
            throw new ToolExecutionException(e.getMessage(), false);
        }
    }

    private String stringArgument(ToolCallRequest request, String key, String defaultValue) {
        Object value = request.arguments().get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString().trim();
        return text.isBlank() ? defaultValue : text;
    }

    private List<Double> numberListArgument(ToolCallRequest request, String key) {
        Object value = request.arguments().get(key);
        if (value == null) {
            return List.of();
        }
        List<Double> parsed = new ArrayList<>();
        if (value instanceof List<?> rawList) {
            for (Object item : rawList) {
                Double number = parseDouble(item);
                if (number != null && number > 0) {
                    parsed.add(number);
                }
            }
            return parsed;
        }
        if (value instanceof String text) {
            for (String token : text.split(",")) {
                Double number = parseDouble(token);
                if (number != null && number > 0) {
                    parsed.add(number);
                }
            }
        }
        return parsed;
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
