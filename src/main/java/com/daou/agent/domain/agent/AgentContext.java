package com.daou.agent.domain.agent;

import com.daou.agent.domain.session.SessionMessage;
import com.daou.agent.domain.tool.ToolCallResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AgentContext {
    private final String sessionId;
    private final String summary;
    private final List<SessionMessage> recentMessages;
    private final String currentUserMessage;
    private final List<ToolCallResult> toolResults = new ArrayList<>();

    public AgentContext(String sessionId, String summary, List<SessionMessage> recentMessages, String currentUserMessage) {
        this.sessionId = sessionId;
        this.summary = summary == null ? "" : summary;
        this.recentMessages = List.copyOf(recentMessages);
        this.currentUserMessage = currentUserMessage;
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

    public List<ToolCallResult> getToolResults() {
        return Collections.unmodifiableList(toolResults);
    }

    public void addToolResult(ToolCallResult result) {
        toolResults.add(result);
    }
}
