package com.daou.agent.domain.tool;

import java.util.Map;

public record ToolCallResult(
        String toolName,
        String status,
        String message,
        Map<String, Object> rawData,
        boolean retriable
) {
    public ToolCallResult {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        status = status == null || status.isBlank() ? "ok" : status;
        message = message == null ? "" : message;
        rawData = rawData == null ? Map.of() : Map.copyOf(rawData);
    }
}
