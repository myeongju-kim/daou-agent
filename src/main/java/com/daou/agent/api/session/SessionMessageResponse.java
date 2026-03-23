package com.daou.agent.api.session;

import java.time.Instant;

public record SessionMessageResponse(
        String role,
        String type,
        String content,
        Instant createdAt
) {
}
