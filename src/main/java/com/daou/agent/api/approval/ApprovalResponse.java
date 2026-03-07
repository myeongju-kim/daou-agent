package com.daou.agent.api.approval;

public record ApprovalResponse(
        String approvalId,
        String status,
        String message
) {
}
