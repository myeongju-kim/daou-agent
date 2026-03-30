package com.daou.agent.api.approval;

public record ApprovalResponse(
        String version,
        String approvalId,
        String status,
        String message,
        String correlationId,
        ApprovalInfoResponse approval
) {
}
