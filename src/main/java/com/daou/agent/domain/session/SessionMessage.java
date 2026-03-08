package com.daou.agent.domain.session;

import com.daou.agent.domain.common.MessageRole;
import com.daou.agent.domain.common.MessageType;
import java.time.Instant;
import java.util.Objects;

public record SessionMessage(
        MessageRole role,
        MessageType type,
        String content,
        Instant createdAt
) {
    public SessionMessage {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("message content must not be blank");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static SessionMessage text(MessageRole role, String content) {
        return new SessionMessage(role, MessageType.TEXT, content, Instant.now());
    }

    public static SessionMessage toolResult(String content) {
        return new SessionMessage(MessageRole.TOOL, MessageType.TOOL_RESULT, content, Instant.now());
    }
}
