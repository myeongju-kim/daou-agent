package com.daou.agent.domain.agent;

import com.daou.agent.domain.session.SessionMessage;
import com.daou.agent.domain.tool.ToolCallResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AgentContext {
    private final String sessionId;
    private final String summary;
    private final List<SessionMessage> recentMessages;
    private final String currentUserMessage;
    private final String selectedModel;
    private final String agentKey;
    private final String intent;
    private final String agentSystemHint;
    private final Set<String> allowedToolNames;
    private final List<ToolCallResult> toolResults = new ArrayList<>();

    public AgentContext(
            String sessionId,
            String summary,
            List<SessionMessage> recentMessages,
            String currentUserMessage,
            String selectedModel
    ) {
        this(
                sessionId,
                summary,
                recentMessages,
                currentUserMessage,
                selectedModel,
                "personal",
                "",
                "",
                Set.of()
        );
    }

    public AgentContext(
            String sessionId,
            String summary,
            List<SessionMessage> recentMessages,
            String currentUserMessage,
            String selectedModel,
            String agentKey,
            String intent,
            String agentSystemHint,
            Set<String> allowedToolNames
    ) {
        this.sessionId = sessionId;
        this.summary = summary == null ? "" : summary;
        this.recentMessages = List.copyOf(recentMessages);
        this.currentUserMessage = currentUserMessage;
        this.selectedModel = selectedModel == null ? "" : selectedModel.trim();
        this.agentKey = agentKey == null || agentKey.isBlank() ? "personal" : agentKey.trim();
        this.intent = intent == null ? "" : intent.trim();
        this.agentSystemHint = agentSystemHint == null ? "" : agentSystemHint.trim();
        this.allowedToolNames = allowedToolNames == null ? Set.of() : Set.copyOf(allowedToolNames);
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSummary() {
        return summary;
    }

    public List<SessionMessage> getRecentMessages() {
        return recentMessages;
    }

    public String getCurrentUserMessage() {
        return currentUserMessage;
    }

    public String getSelectedModel() {
        return selectedModel;
    }

    public String getAgentKey() {
        return agentKey;
    }

    public String getIntent() {
        return intent;
    }

    public String getAgentSystemHint() {
        return agentSystemHint;
    }

    public Set<String> getAllowedToolNames() {
        return allowedToolNames;
    }

    public boolean isToolAllowed(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return false;
        }
        if (allowedToolNames.isEmpty()) {
            return true;
        }
        return allowedToolNames.contains(toolName);
    }

    public List<ToolCallResult> getToolResults() {
        return Collections.unmodifiableList(toolResults);
    }

    public void addToolResult(ToolCallResult result) {
        toolResults.add(result);
    }
}
