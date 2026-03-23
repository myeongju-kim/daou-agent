package com.daou.agent.api.session;

import java.time.Instant;
import java.util.List;

public record SessionDetailResponse(
        String version,
        String sessionId,
        String agentKey,
        String title,
        String summary,
        String selectedModel,
        Instant createdAt,
        Instant updatedAt,
        List<SessionMessageResponse> messages,
        String correlationId
) {
}
