package com.daou.agent.application.session;

import com.daou.agent.domain.session.SessionMessage;
import java.time.Instant;
import java.util.List;

public record SessionDetailView(
        String sessionId,
        String agentKey,
        String title,
        String summary,
        String selectedModel,
        Instant createdAt,
        Instant updatedAt,
        List<SessionMessage> messages
) {
}
