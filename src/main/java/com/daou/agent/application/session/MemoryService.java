package com.daou.agent.application.session;

import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.session.Session;
import com.daou.agent.domain.session.SessionMessage;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {
    private final int recentMessageCount;

    public MemoryService(@Value("${agent.recent-message-count:12}") int recentMessageCount) {
        this.recentMessageCount = recentMessageCount;
    }

    public AgentContext buildContext(Session session, String currentUserMessage) {
        List<SessionMessage> messages = session.getMessages();
        int from = Math.max(0, messages.size() - recentMessageCount);
        List<SessionMessage> recent = messages.subList(from, messages.size());
        return new AgentContext(session.getId(), session.getSummary(), recent, currentUserMessage);
    }
}
