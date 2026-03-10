package com.daou.agent.api.dashboard;

import com.daou.agent.api.common.ApiContract;
import com.daou.agent.application.dashboard.DashboardService;
import com.daou.agent.application.dashboard.DashboardSummary;
import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        DashboardSummary summary = dashboardService.getSummary();
        return new DashboardResponse(
                ApiContract.VERSION,
                summary.sessionCount(),
                summary.pendingApprovalCount(),
                summary.llmProvider(),
                summary.tools(),
                CorrelationIdHolder.getOrCreate()
        );
    }
}
