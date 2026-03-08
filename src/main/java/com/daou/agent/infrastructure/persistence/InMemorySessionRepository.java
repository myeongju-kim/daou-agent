package com.daou.agent.infrastructure.persistence;

import com.daou.agent.application.port.SessionRepository;
import com.daou.agent.domain.session.Session;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySessionRepository implements SessionRepository {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session getOrCreate(String sessionId) {
        return sessions.computeIfAbsent(sessionId, Session::new);
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public long count() {
        return sessions.size();
    }
}
