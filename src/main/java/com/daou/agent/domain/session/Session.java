package com.daou.agent.domain.session;

import com.daou.agent.domain.common.MessageRole;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Session {
    public static final String DEFAULT_AGENT_KEY = "daouoffice";

    private final String id;
    private final String agentKey;
    private final Instant createdAt;
    private final List<SessionMessage> messages = new ArrayList<>();
    private String title = "";
    private String summary = "";
    private String selectedModel = "";
    private Instant updatedAt;

    public Session(String id) {
        this(id, DEFAULT_AGENT_KEY);
    }

    public Session(String id, String agentKey) {
        this(id, agentKey, "", "", "", Instant.now(), Instant.now(), List.of());
    }

    private Session(
            String id,
            String agentKey,
            String title,
            String summary,
            String selectedModel,
            Instant createdAt,
            Instant updatedAt,
            List<SessionMessage> messages
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("session id must not be blank");
        }
        this.id = id;
        this.agentKey = normalizeAgentKey(agentKey);
        this.title = Objects.requireNonNullElse(title, "");
        this.summary = Objects.requireNonNullElse(summary, "");
        this.selectedModel = selectedModel == null ? "" : selectedModel.trim();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (messages != null) {
            this.messages.addAll(messages);
        }
    }

    public static Session restore(
            String id,
            String agentKey,
            String title,
            String summary,
            String selectedModel,
            Instant createdAt,
            Instant updatedAt,
            List<SessionMessage> messages
    ) {
        return new Session(id, agentKey, title, summary, selectedModel, createdAt, updatedAt, messages);
    }

    public String getId() {
        return id;
    }

    public String getAgentKey() {
        return agentKey;
    }

    public String getTitle() {
        return title;
    }

    public List<SessionMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void appendUserMessage(String content) {
        messages.add(SessionMessage.text(MessageRole.USER, content));
        if (title.isBlank()) {
            title = deriveTitle(content);
        }
        touch();
    }

    public void appendAssistantMessage(String content) {
        messages.add(SessionMessage.text(MessageRole.ASSISTANT, content));
        touch();
    }

    public void appendToolResult(String content) {
        messages.add(SessionMessage.toolResult(content));
        touch();
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
        touch();
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public void setSelectedModel(String selectedModel) {
        this.selectedModel = selectedModel == null ? "" : selectedModel.trim();
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    private String deriveTitle(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isBlank()) {
            return "";
        }
        return trimmed.length() <= 60 ? trimmed : trimmed.substring(0, 60);
    }

    private static String normalizeAgentKey(String agentKey) {
        if (agentKey == null || agentKey.isBlank()) {
            return DEFAULT_AGENT_KEY;
        }
        return agentKey.trim();
    }
}
