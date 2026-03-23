package com.daou.agent.infrastructure.persistence;

import com.daou.agent.application.port.ApprovalRepository;
import com.daou.agent.domain.approval.ApprovalRequest;
import com.daou.agent.domain.common.ApprovalStatus;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "agent.storage.type", havingValue = "jdbc", matchIfMissing = true)
public class JdbcApprovalRepository implements ApprovalRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcApprovalRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ApprovalRequest save(ApprovalRequest request) {
        String argumentsJson = toJson(request.getToolCall().arguments());
        int updated = jdbcTemplate.update("""
                        UPDATE agent_approval_requests
                        SET session_id = ?, tool_name = ?, tool_arguments = ?, risk_level = ?, requested_user_message = ?,
                            summary_snapshot = ?, selected_model_snapshot = ?, created_correlation_id = ?, created_at = ?,
                            status = ?, decided_at = ?
                        WHERE id = ?
                        """,
                request.getSessionId(),
                request.getToolCall().toolName(),
                argumentsJson,
                request.getRiskLevel().name(),
                request.getRequestedUserMessage(),
                request.getSummarySnapshot(),
                request.getSelectedModelSnapshot(),
                request.getCreatedCorrelationId(),
                Timestamp.from(request.getCreatedAt()),
                request.getStatus().name(),
                toTimestamp(request.getDecidedAt()),
                request.getId()
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                            INSERT INTO agent_approval_requests(
                                id, session_id, tool_name, tool_arguments, risk_level, requested_user_message,
                                summary_snapshot, selected_model_snapshot, created_correlation_id, created_at, status, decided_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    request.getId(),
                    request.getSessionId(),
                    request.getToolCall().toolName(),
                    argumentsJson,
                    request.getRiskLevel().name(),
                    request.getRequestedUserMessage(),
                    request.getSummarySnapshot(),
                    request.getSelectedModelSnapshot(),
                    request.getCreatedCorrelationId(),
                    Timestamp.from(request.getCreatedAt()),
                    request.getStatus().name(),
                    toTimestamp(request.getDecidedAt())
            );
        }
        return request;
    }

    @Override
    public Optional<ApprovalRequest> findById(String id) {
        List<ApprovalRequest> approvals = jdbcTemplate.query("""
                        SELECT id, session_id, tool_name, tool_arguments, risk_level, requested_user_message,
                               summary_snapshot, selected_model_snapshot, created_correlation_id, created_at, status, decided_at
                        FROM agent_approval_requests
                        WHERE id = ?
                        """,
                (rs, rowNum) -> mapApprovalRequest(rs),
                id
        );
        return approvals.stream().findFirst();
    }

    @Override
    public long countPending() {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM agent_approval_requests
                        WHERE status = ?
                        """,
                Long.class,
                ApprovalStatus.PENDING.name()
        );
        return count == null ? 0L : count;
    }

    private ApprovalRequest mapApprovalRequest(ResultSet rs) throws SQLException {
        return ApprovalRequest.restore(
                rs.getString("id"),
                rs.getString("session_id"),
                new ToolCallRequest(rs.getString("tool_name"), toMap(rs.getString("tool_arguments"))),
                RiskLevel.valueOf(rs.getString("risk_level")),
                rs.getString("requested_user_message"),
                rs.getString("summary_snapshot"),
                rs.getString("selected_model_snapshot"),
                rs.getString("created_correlation_id"),
                toInstant(rs.getTimestamp("created_at")),
                ApprovalStatus.valueOf(rs.getString("status")),
                toInstant(rs.getTimestamp("decided_at"))
        );
    }

    private String toJson(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsString(arguments == null ? Map.of() : arguments);
        } catch (Exception e) {
            throw new IllegalStateException("approval tool arguments 직렬화에 실패했습니다.", e);
        }
    }

    private Map<String, Object> toMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("approval tool arguments 역직렬화에 실패했습니다.", e);
        }
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
