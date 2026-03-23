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
    private final String requestedUserMessage;
    private final String summarySnapshot;
    private final String selectedModelSnapshot;
    private final String createdCorrelationId;
    private final Instant createdAt;
    private ApprovalStatus status;
    private Instant decidedAt;

    public ApprovalRequest(
            String sessionId,
            ToolCallRequest toolCall,
            RiskLevel riskLevel,
            String requestedUserMessage,
            String summarySnapshot,
            String selectedModelSnapshot,
            String createdCorrelationId
    ) {
        this(
                UUID.randomUUID().toString(),
                sessionId,
                toolCall,
                riskLevel,
                requestedUserMessage,
                summarySnapshot,
                selectedModelSnapshot,
                createdCorrelationId,
                Instant.now(),
                ApprovalStatus.PENDING,
                null
        );
    }

    private ApprovalRequest(
            String id,
            String sessionId,
            ToolCallRequest toolCall,
            RiskLevel riskLevel,
            String requestedUserMessage,
            String summarySnapshot,
            String selectedModelSnapshot,
            String createdCorrelationId,
            Instant createdAt,
            ApprovalStatus status,
            Instant decidedAt
    ) {
        this.id = id;
        this.sessionId = sessionId;
        this.toolCall = toolCall;
        this.riskLevel = riskLevel;
        this.requestedUserMessage = requestedUserMessage == null ? "" : requestedUserMessage;
        this.summarySnapshot = summarySnapshot == null ? "" : summarySnapshot;
        this.selectedModelSnapshot = selectedModelSnapshot == null ? "" : selectedModelSnapshot;
        this.createdCorrelationId = createdCorrelationId == null ? "" : createdCorrelationId;
        this.createdAt = createdAt;
        this.status = status == null ? ApprovalStatus.PENDING : status;
        this.decidedAt = decidedAt;
    }

    public static ApprovalRequest restore(
            String id,
            String sessionId,
            ToolCallRequest toolCall,
            RiskLevel riskLevel,
            String requestedUserMessage,
            String summarySnapshot,
            String selectedModelSnapshot,
            String createdCorrelationId,
            Instant createdAt,
            ApprovalStatus status,
            Instant decidedAt
    ) {
        return new ApprovalRequest(
                id,
                sessionId,
                toolCall,
                riskLevel,
                requestedUserMessage,
                summarySnapshot,
                selectedModelSnapshot,
                createdCorrelationId,
                createdAt,
                status,
                decidedAt
        );
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

    public String getRequestedUserMessage() {
        return requestedUserMessage;
    }

    public String getSummarySnapshot() {
        return summarySnapshot;
    }

    public String getSelectedModelSnapshot() {
        return selectedModelSnapshot;
    }

    public String getCreatedCorrelationId() {
        return createdCorrelationId;
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

    public synchronized ApprovalStatus approve() {
        if (status == ApprovalStatus.REJECTED) {
            throw new IllegalStateException("이미 거절된 승인 요청은 승인할 수 없습니다.");
        }
        if (status == ApprovalStatus.PENDING) {
            this.status = ApprovalStatus.APPROVED;
            this.decidedAt = Instant.now();
        }
        return this.status;
    }

    public synchronized ApprovalStatus reject() {
        if (status == ApprovalStatus.APPROVED) {
            throw new IllegalStateException("이미 승인된 요청은 거절할 수 없습니다.");
        }
        if (status == ApprovalStatus.PENDING) {
            this.status = ApprovalStatus.REJECTED;
            this.decidedAt = Instant.now();
        }
        return this.status;
    }
}
