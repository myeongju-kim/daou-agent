package com.daou.agent.domain.tool;

import com.daou.agent.domain.common.RiskLevel;
import java.util.Objects;

public record ToolDefinition(String name, String description, RiskLevel riskLevel) {
    public ToolDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("tool name must not be blank");
        }
        description = Objects.requireNonNullElse(description, "");
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
    }
}
