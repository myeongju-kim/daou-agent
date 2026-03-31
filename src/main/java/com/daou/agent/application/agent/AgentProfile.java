package com.daou.agent.application.agent;

import java.util.List;
import java.util.Set;

public record AgentProfile(
        String key,
        String name,
        String description,
        String systemHint,
        Set<String> allowedTools,
        List<String> supportedIntents
) {
    public AgentProfile {
        key = normalize(key, "personal");
        name = normalize(name, key);
        description = normalize(description, "");
        systemHint = normalize(systemHint, "");
        allowedTools = allowedTools == null ? Set.of() : Set.copyOf(allowedTools);
        supportedIntents = supportedIntents == null ? List.of() : List.copyOf(supportedIntents);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
