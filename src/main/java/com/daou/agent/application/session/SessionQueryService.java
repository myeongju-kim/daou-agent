package com.daou.agent.application.session;

import com.daou.agent.application.port.SessionRepository;
import com.daou.agent.domain.session.Session;
import com.daou.agent.domain.session.SessionMessage;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class SessionQueryService {

    private final SessionRepository sessionRepository;

    public SessionQueryService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public List<SessionSummaryView> getSessions(String agentKey, int limit) {
        return sessionRepository.findAllByAgentKey(normalizeAgentKey(agentKey), limit).stream()
                .map(this::toSummary)
                .toList();
    }

    public SessionDetailView getSession(String agentKey, String sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .filter(candidate -> candidate.getAgentKey().equals(normalizeAgentKey(agentKey)))
                .orElseThrow(() -> new NoSuchElementException("session not found: " + sessionId));

        return new SessionDetailView(
                session.getId(),
                session.getAgentKey(),
                session.getTitle(),
                session.getSummary(),
                session.getSelectedModel(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                session.getMessages()
        );
    }

    private SessionSummaryView toSummary(Session session) {
        List<SessionMessage> messages = session.getMessages();
        String lastMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).content();
        return new SessionSummaryView(
                session.getId(),
                session.getAgentKey(),
                session.getTitle(),
                session.getSummary(),
                session.getSelectedModel(),
                lastMessage,
                messages.size(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private String normalizeAgentKey(String agentKey) {
        if (agentKey == null || agentKey.isBlank()) {
            return Session.DEFAULT_AGENT_KEY;
        }
        return agentKey.trim();
    }
}
