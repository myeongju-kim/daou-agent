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

    public void appendUserMessage(String sessionId, String message) {
        Session session = getOrCreate(sessionId);
        session.appendUserMessage(message);
    }

    public void appendAssistantMessage(String sessionId, String message) {
        Session session = getOrCreate(sessionId);
        session.appendAssistantMessage(message);
    }

    public void appendToolResult(String sessionId, String message) {
        Session session = getOrCreate(sessionId);
        session.appendToolResult(message);
    }

    public void setSelectedModel(String sessionId, String model) {
        Session session = getOrCreate(sessionId);
        session.setSelectedModel(model);
    }

    public String getSelectedModel(String sessionId) {
        Session session = getOrCreate(sessionId);
        return session.getSelectedModel();
    }

    public long countSessions() {
        return sessionRepository.count();
    }
}
