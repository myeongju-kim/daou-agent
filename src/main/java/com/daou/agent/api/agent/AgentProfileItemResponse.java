package com.daou.agent.api.agent;

import java.util.List;
import java.util.Set;

public record AgentProfileItemResponse(
        String key,
        String name,
        String description,
        boolean isDefault,
        List<String> supportedIntents,
        Set<String> allowedTools
) {
}
