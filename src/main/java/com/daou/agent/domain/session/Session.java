package com.daou.agent.domain.session;

import com.daou.agent.domain.common.MessageRole;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Session {
    private final String id;
    private final List<SessionMessage> messages = new ArrayList<>();
    private String summary = "";

    public Session(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("session id must not be blank");
        }
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<SessionMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void appendUserMessage(String content) {
        messages.add(SessionMessage.text(MessageRole.USER, content));
    }

    public void appendAssistantMessage(String content) {
        messages.add(SessionMessage.text(MessageRole.ASSISTANT, content));
    }

    public void appendToolResult(String content) {
        messages.add(SessionMessage.toolResult(content));
    }

    public Optional<String> latestUserMessage() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            SessionMessage message = messages.get(i);
            if (message.role() == MessageRole.USER) {
                return Optional.of(message.content());
            }
        }
        return Optional.empty();
    }

    public String getSummary() {
        return summary;
    }

    public void updateSummary(String summary) {
        this.summary = Objects.requireNonNullElse(summary, "");
    }
}
