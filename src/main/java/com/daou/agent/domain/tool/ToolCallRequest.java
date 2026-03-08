package com.daou.agent.domain.tool;

import java.util.Map;

public record ToolCallRequest(String toolName, Map<String, Object> arguments) {
    public ToolCallRequest {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
