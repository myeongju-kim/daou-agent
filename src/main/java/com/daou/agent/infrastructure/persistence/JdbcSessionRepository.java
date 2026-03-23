package com.daou.agent.infrastructure.persistence;

import com.daou.agent.application.port.SessionRepository;
import com.daou.agent.domain.common.MessageRole;
import com.daou.agent.domain.common.MessageType;
import com.daou.agent.domain.session.Session;
import com.daou.agent.domain.session.SessionMessage;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "agent.storage.type", havingValue = "jdbc", matchIfMissing = true)
public class JdbcSessionRepository implements SessionRepository {

    private static final RowMapper<SessionMessage> MESSAGE_ROW_MAPPER = (rs, rowNum) -> new SessionMessage(
            MessageRole.valueOf(rs.getString("role")),
            MessageType.valueOf(rs.getString("type")),
            rs.getString("content"),
            toInstant(rs.getTimestamp("created_at"))
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Session getOrCreate(String sessionId, String agentKey) {
        return findById(sessionId).orElseGet(() -> create(sessionId, agentKey));
    }

    @Override
    public Optional<Session> findById(String sessionId) {
        List<Session> sessions = jdbcTemplate.query("""
                        SELECT id, agent_key, title, summary, selected_model, created_at, updated_at
                        FROM agent_sessions
                        WHERE id = ?
                        """,
                (rs, rowNum) -> mapSession(rs),
                sessionId
        );
        if (sessions.isEmpty()) {
            return Optional.empty();
        }
        Session session = sessions.get(0);
        return Optional.of(Session.restore(
                session.getId(),
                session.getAgentKey(),
                session.getTitle(),
                session.getSummary(),
                session.getSelectedModel(),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                loadMessages(sessionId)
        ));
    }

    @Override
    public List<Session> findAllByAgentKey(String agentKey, int limit) {
        List<Session> sessions = jdbcTemplate.query("""
                        SELECT id, agent_key, title, summary, selected_model, created_at, updated_at
                        FROM agent_sessions
                        WHERE agent_key = ?
                        ORDER BY updated_at DESC, created_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> mapSession(rs),
                agentKey,
                limit
        );
        return sessions.stream()
                .map(session -> Session.restore(
                        session.getId(),
                        session.getAgentKey(),
                        session.getTitle(),
                        session.getSummary(),
                        session.getSelectedModel(),
                        session.getCreatedAt(),
                        session.getUpdatedAt(),
                        loadMessages(session.getId())
                ))
                .sorted(Comparator.comparing(Session::getUpdatedAt).reversed())
                .toList();
    }

    private List<SessionMessage> loadMessages(String sessionId) {
        return jdbcTemplate.query("""
                        SELECT role, type, content, created_at
                        FROM agent_session_messages
                        WHERE session_id = ?
                        ORDER BY message_order ASC, id ASC
                        """,
                MESSAGE_ROW_MAPPER,
                sessionId
        );
    }

    @Override
    public Session save(Session session) {
        int updated = jdbcTemplate.update("""
                        UPDATE agent_sessions
                        SET agent_key = ?, title = ?, summary = ?, selected_model = ?, created_at = ?, updated_at = ?
                        WHERE id = ?
                        """,
                session.getAgentKey(),
                session.getTitle(),
                session.getSummary(),
                session.getSelectedModel(),
                Timestamp.from(session.getCreatedAt()),
                Timestamp.from(session.getUpdatedAt()),
                session.getId()
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                            INSERT INTO agent_sessions(id, agent_key, title, summary, selected_model, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    session.getId(),
                    session.getAgentKey(),
                    session.getTitle(),
                    session.getSummary(),
                    session.getSelectedModel(),
                    Timestamp.from(session.getCreatedAt()),
                    Timestamp.from(session.getUpdatedAt())
            );
        }

        jdbcTemplate.update("DELETE FROM agent_session_messages WHERE session_id = ?", session.getId());
        List<SessionMessage> messages = session.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            SessionMessage message = messages.get(i);
            jdbcTemplate.update("""
                            INSERT INTO agent_session_messages(session_id, message_order, role, type, content, created_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    session.getId(),
                    i,
                    message.role().name(),
                    message.type().name(),
                    message.content(),
                    Timestamp.from(message.createdAt())
            );
        }
        return session;
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_sessions", Long.class);
        return count == null ? 0L : count;
    }

    private Session create(String sessionId, String agentKey) {
        Session session = new Session(sessionId, agentKey);
        try {
            return save(session);
        } catch (DuplicateKeyException ignored) {
            return findById(sessionId).orElseThrow();
        }
    }

    private Session mapSession(ResultSet rs) throws SQLException {
        return Session.restore(
                rs.getString("id"),
                rs.getString("agent_key"),
                rs.getString("title"),
                rs.getString("summary"),
                rs.getString("selected_model"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at")),
                List.of()
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? Instant.now() : timestamp.toInstant();
    }
}
