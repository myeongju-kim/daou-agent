package com.daou.agent.infrastructure.persistence;

import com.daou.agent.application.port.SessionRepository;
import com.daou.agent.domain.session.Session;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "agent.storage.type", havingValue = "memory")
public class InMemorySessionRepository implements SessionRepository {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session getOrCreate(String sessionId, String agentKey) {
        return sessions.computeIfAbsent(sessionId, id -> new Session(id, agentKey));
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public Session save(Session session) {
        sessions.put(session.getId(), session);
        return session;
    }

    @Override
    public long count() {
        return sessions.size();
    }
}
