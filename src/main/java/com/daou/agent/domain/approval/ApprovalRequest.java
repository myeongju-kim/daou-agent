package com.daou.agent.domain.approval;

import com.daou.agent.domain.common.ApprovalStatus;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolCallRequest;
import java.time.Instant;
import java.util.UUID;

public class ApprovalRequest {
    private final String id;
    private final String sessionId;
    private final ToolCallRequest toolCall;
    private final RiskLevel riskLevel;
    private final Instant createdAt;
    private ApprovalStatus status;
    private Instant decidedAt;

    public ApprovalRequest(String sessionId, ToolCallRequest toolCall, RiskLevel riskLevel) {
        this.id = UUID.randomUUID().toString();
        this.sessionId = sessionId;
        this.toolCall = toolCall;
        this.riskLevel = riskLevel;
        this.createdAt = Instant.now();
        this.status = ApprovalStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ToolCallRequest getToolCall() {
        return toolCall;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void approve() {
        this.status = ApprovalStatus.APPROVED;
        this.decidedAt = Instant.now();
    }

    public void reject() {
        this.status = ApprovalStatus.REJECTED;
        this.decidedAt = Instant.now();
    }
}
