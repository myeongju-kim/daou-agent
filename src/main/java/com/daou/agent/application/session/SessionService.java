package com.daou.agent.application.session;

import com.daou.agent.application.port.SessionRepository;
import com.daou.agent.domain.session.Session;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Session getOrCreate(String sessionId) {
        return sessionRepository.getOrCreate(sessionId);
    }

    public Session getOrCreate(String sessionId, String agentKey) {
        return sessionRepository.getOrCreate(sessionId, agentKey);
    }

    public void appendUserMessage(String sessionId, String message) {
        Session session = getOrCreate(sessionId);
        session.appendUserMessage(message);
        sessionRepository.save(session);
    }

    public void appendAssistantMessage(String sessionId, String message) {
        Session session = getOrCreate(sessionId);
        session.appendAssistantMessage(message);
        sessionRepository.save(session);
    }

    public void appendToolResult(String sessionId, String message) {
        Session session = getOrCreate(sessionId);
        session.appendToolResult(message);
        sessionRepository.save(session);
    }

    public void setSelectedModel(String sessionId, String model) {
        Session session = getOrCreate(sessionId);
        session.setSelectedModel(model);
        sessionRepository.save(session);
    }

    public String getSelectedModel(String sessionId) {
        Session session = getOrCreate(sessionId);
        return session.getSelectedModel();
    }

    public long countSessions() {
        return sessionRepository.count();
    }
}
