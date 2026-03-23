package com.daou.agent.api.session;

import java.time.Instant;

public record SessionListItemResponse(
        String sessionId,
        String agentKey,
        String title,
        String summary,
        String selectedModel,
        String lastMessage,
        int messageCount,
        Instant createdAt,
        Instant updatedAt
) {
}
