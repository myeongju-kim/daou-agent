package com.daou.agent.application.approval;

import com.daou.agent.application.agent.AgentLoopEngine;
import com.daou.agent.application.agent.AgentProfile;
import com.daou.agent.application.agent.AgentProfileService;
import com.daou.agent.application.agent.IntentRuleResolver;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApprovalResumeService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalResumeService.class);

    private final ApprovalService approvalService;
    private final SessionService sessionService;
    private final MemoryService memoryService;
    private final ToolExecutor toolExecutor;
    private final AgentLoopEngine agentLoopEngine;
    private final AgentProfileService agentProfileService;
    private final IntentRuleResolver intentRuleResolver;
    private final Map<String, AgentResult> cachedResumeResults = new ConcurrentHashMap<>();
    private final Map<String, Object> approvalLocks = new ConcurrentHashMap<>();

    public ApprovalResumeService(
            ApprovalService approvalService,
            SessionService sessionService,
            MemoryService memoryService,
            ToolExecutor toolExecutor,
            AgentLoopEngine agentLoopEngine,
            AgentProfileService agentProfileService,
            IntentRuleResolver intentRuleResolver
    ) {
        this.approvalService = approvalService;
        this.sessionService = sessionService;
        this.memoryService = memoryService;
        this.toolExecutor = toolExecutor;
        this.agentLoopEngine = agentLoopEngine;
        this.agentProfileService = agentProfileService;
        this.intentRuleResolver = intentRuleResolver;
    }

    public AgentResult approveAndResume(String approvalId) {
        AgentResult cached = cachedResumeResults.get(approvalId);
        if (cached != null) {
            log.info("event=approval.resume.cached approvalId={}", approvalId);
            return cached;
        }

        Object lock = approvalLocks.computeIfAbsent(approvalId, key -> new Object());
        synchronized (lock) {
            cached = cachedResumeResults.get(approvalId);
            if (cached != null) {
                return cached;
            }

            log.info("event=approval.resume.start approvalId={}", approvalId);
            ApprovalRequest approvalRequest = approvalService.approveAndGet(approvalId);
            Session session = sessionService.getOrCreate(approvalRequest.getSessionId());
            String latestUserMessage = session.latestUserMessage()
                    .orElseThrow(() -> new IllegalStateException("세션에 사용자 메시지가 없어 재개할 수 없습니다."));

            validateContext(approvalRequest, session, latestUserMessage);

            List<LoopStep> steps = new ArrayList<>();
            steps.add(new LoopStep("approval_approved", approvalId));

            ToolCallResult toolResult = toolExecutor.execute(approvalRequest.getToolCall());
            steps.add(new LoopStep("tool_result", toolResult.toolName() + ": " + toolResult.message()));
            sessionService.appendToolResult(session.getId(), toolResult.message());

            session = sessionService.getOrCreate(approvalRequest.getSessionId());
            AgentProfile profile = agentProfileService.getProfile(session.getAgentKey());
            String intent = intentRuleResolver.resolve(session.getAgentKey(), latestUserMessage);
            AgentContext context = memoryService.buildContext(
                    session,
                    latestUserMessage,
                    session.getAgentKey(),
                    intent,
                    profile.allowedTools(),
                    profile.systemHint()
            );
            context.addToolResult(toolResult);

            AgentResult resumed = agentLoopEngine.execute(context);
            steps.addAll(resumed.steps());

            if (resumed.status() == AgentStatus.OK) {
                sessionService.appendAssistantMessage(session.getId(), resumed.message());
            }

            AgentResult finalResult = new AgentResult(resumed.status(), resumed.message(), steps, resumed.approvalId());
            cachedResumeResults.put(approvalId, finalResult);
            approvalLocks.remove(approvalId);
            log.info("event=approval.resume.finish approvalId={} status={}", approvalId, finalResult.status().name());
            return finalResult;
        }
    }

    private void validateContext(ApprovalRequest approvalRequest, Session session, String latestUserMessage) {
        if (!latestUserMessage.equals(approvalRequest.getRequestedUserMessage())) {
            throw new IllegalStateException("승인 대기 이후 사용자 메시지가 변경되어 재개할 수 없습니다.");
        }

        if (!session.getSelectedModel().equals(approvalRequest.getSelectedModelSnapshot())) {
            log.warn("event=approval.resume.model_changed approvalId={} before={} current={}",
                    approvalRequest.getId(),
                    approvalRequest.getSelectedModelSnapshot(),
                    session.getSelectedModel());
        }

        if (!session.getSummary().equals(approvalRequest.getSummarySnapshot())) {
            log.warn("event=approval.resume.summary_changed approvalId={}", approvalRequest.getId());
        }
    }
}
