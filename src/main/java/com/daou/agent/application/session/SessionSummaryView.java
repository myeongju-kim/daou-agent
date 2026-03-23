package com.daou.agent.application.session;

import java.time.Instant;

public record SessionSummaryView(
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
