package com.daou.agent.application.dashboard;

import java.util.Set;

public record DashboardSummary(
        long sessionCount,
        long pendingApprovalCount,
        String llmProvider,
        Set<String> tools
) {
}
