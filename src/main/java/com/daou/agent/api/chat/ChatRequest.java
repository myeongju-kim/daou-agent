package com.daou.agent.api.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String sessionId,
        @NotBlank String message,
        String agentKey
) {
    public String normalizedAgentKey() {
        if (agentKey == null || agentKey.isBlank()) {
            return "daouoffice";
        }
        return agentKey.trim();
    }
}
