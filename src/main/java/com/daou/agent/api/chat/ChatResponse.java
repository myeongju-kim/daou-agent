package com.daou.agent.api.chat;

import com.daou.agent.api.approval.ApprovalInfoResponse;
import java.util.List;

public record ChatResponse(
        String version,
        String status,
        String message,
        List<String> steps,
        String approvalId,
        String correlationId,
        ApprovalInfoResponse approval
) {
}
