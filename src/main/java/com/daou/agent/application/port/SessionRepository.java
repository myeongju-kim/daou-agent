package com.daou.agent.application.port;

import com.daou.agent.domain.session.Session;
import java.util.List;
import java.util.Optional;

public interface SessionRepository {
    default Session getOrCreate(String sessionId) {
        return getOrCreate(sessionId, Session.DEFAULT_AGENT_KEY);
    }

    Session getOrCreate(String sessionId, String agentKey);

    Optional<Session> findById(String sessionId);

    List<Session> findAllByAgentKey(String agentKey, int limit);

    Session save(Session session);

    long count();
}
