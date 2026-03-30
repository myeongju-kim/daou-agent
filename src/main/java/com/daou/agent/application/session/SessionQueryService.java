package com.daou.agent.application.session;

import com.daou.agent.application.agent.AgentProfileService;
import com.daou.agent.application.port.SessionRepository;
import com.daou.agent.domain.session.Session;
import com.daou.agent.domain.session.SessionMessage;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class SessionQueryService {

    private final SessionRepository sessionRepository;
    private final AgentProfileService agentProfileService;

    public SessionQueryService(SessionRepository sessionRepository, AgentProfileService agentProfileService) {
        this.sessionRepository = sessionRepository;
        this.agentProfileService = agentProfileService;
    }

    public List<SessionSummaryView> getSessions(String agentKey, int limit) {
        String normalizedAgentKey = normalizeAgentKey(agentKey);
        return sessionRepository.findAllByAgentKey(normalizedAgentKey, limit).stream()
                .map(this::toSummary)
                .toList();
    }

    public SessionDetailView getSession(String agentKey, String sessionId) {
        String normalizedAgentKey = normalizeAgentKey(agentKey);
        String storageSessionId = AgentSessionIdCodec.encode(normalizedAgentKey, sessionId);
        Session session = sessionRepository.findById(storageSessionId)
                .filter(candidate -> candidate.getAgentKey().equals(normalizedAgentKey))
                .orElseThrow(() -> new NoSuchElementException("session not found: " + sessionId));

        return new SessionDetailView(
                AgentSessionIdCodec.decodePublicId(session.getId()),
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
                AgentSessionIdCodec.decodePublicId(session.getId()),
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
        return agentProfileService.normalizeAgentKey(agentKey);
    }
}
