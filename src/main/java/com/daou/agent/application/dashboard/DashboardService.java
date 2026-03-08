package com.daou.agent.application.dashboard;

import com.daou.agent.application.approval.ApprovalService;
import com.daou.agent.application.session.SessionService;
import com.daou.agent.domain.tool.ToolRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final SessionService sessionService;
    private final ApprovalService approvalService;
    private final ToolRegistry toolRegistry;
    private final String llmProvider;

    public DashboardService(
            SessionService sessionService,
            ApprovalService approvalService,
            ToolRegistry toolRegistry,
            @Value("${agent.llm-provider:ollama}") String llmProvider
    ) {
        this.sessionService = sessionService;
        this.approvalService = approvalService;
        this.toolRegistry = toolRegistry;
        this.llmProvider = llmProvider;
    }

    public DashboardSummary getSummary() {
        return new DashboardSummary(
                sessionService.countSessions(),
                approvalService.countPending(),
                llmProvider,
                toolRegistry.names()
        );
    }
}
