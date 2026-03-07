package com.daou.agent.application.port;

import com.daou.agent.domain.session.Session;
import java.util.Optional;

public interface SessionRepository {
    Session getOrCreate(String sessionId);

    Optional<Session> findById(String sessionId);

    long count();
}
