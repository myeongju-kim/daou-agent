package com.daou.agent.application.approval;

import com.daou.agent.application.agent.AgentLoopEngine;
import com.daou.agent.application.session.MemoryService;
import com.daou.agent.application.session.SessionService;
import com.daou.agent.application.port.ToolExecutor;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.AgentStatus;
import com.daou.agent.domain.agent.LoopStep;
import com.daou.agent.domain.approval.ApprovalRequest;
import com.daou.agent.domain.session.Session;
import com.daou.agent.domain.tool.ToolCallResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ApprovalResumeService {

    private final ApprovalService approvalService;
    private final SessionService sessionService;
    private final MemoryService memoryService;
    private final ToolExecutor toolExecutor;
    private final AgentLoopEngine agentLoopEngine;

    public ApprovalResumeService(
            ApprovalService approvalService,
            SessionService sessionService,
            MemoryService memoryService,
            ToolExecutor toolExecutor,
            AgentLoopEngine agentLoopEngine
    ) {
        this.approvalService = approvalService;
        this.sessionService = sessionService;
        this.memoryService = memoryService;
        this.toolExecutor = toolExecutor;
        this.agentLoopEngine = agentLoopEngine;
    }

    public AgentResult approveAndResume(String approvalId) {
        ApprovalRequest approvalRequest = approvalService.approveAndGet(approvalId);
        Session session = sessionService.getOrCreate(approvalRequest.getSessionId());
        String latestUserMessage = session.latestUserMessage()
                .orElseThrow(() -> new IllegalStateException("세션에 사용자 메시지가 없어 재개할 수 없습니다."));

        List<LoopStep> steps = new ArrayList<>();
        steps.add(new LoopStep("approval_approved", approvalId));

        ToolCallResult toolResult = toolExecutor.execute(approvalRequest.getToolCall());
        steps.add(new LoopStep("tool_result", toolResult.toolName() + ": " + toolResult.message()));
        sessionService.appendToolResult(session.getId(), toolResult.message());

        AgentContext context = memoryService.buildContext(session, latestUserMessage);
        context.addToolResult(toolResult);

        AgentResult resumed = agentLoopEngine.execute(context);
        steps.addAll(resumed.steps());

        if (resumed.status() == AgentStatus.OK) {
            sessionService.appendAssistantMessage(session.getId(), resumed.message());
        }

        return new AgentResult(resumed.status(), resumed.message(), steps, resumed.approvalId());
    }
}
