package com.daou.agent.api.dashboard;

import java.util.Set;

public record DashboardResponse(
        String version,
        long sessionCount,
        long pendingApprovalCount,
        String llmProvider,
        Set<String> tools,
        String correlationId
) {
}
