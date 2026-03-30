package com.daou.agent.api.chat;

import com.daou.agent.api.approval.ApprovalInfoResponse;
import com.daou.agent.api.common.ApiContract;
import com.daou.agent.application.agent.AgentRunner;
import com.daou.agent.application.approval.ApprovalService;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.AgentStatus;
import com.daou.agent.domain.agent.LoopStep;
import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final AgentRunner agentRunner;
    private final ApprovalService approvalService;

    public ChatController(AgentRunner agentRunner, ApprovalService approvalService) {
        this.agentRunner = agentRunner;
        this.approvalService = approvalService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        AgentResult result = agentRunner.run(request.normalizedAgentKey(), request.sessionId(), request.message());
        List<String> stepDetails = result.steps().stream()
                .map(LoopStep::detail)
                .toList();
        ApprovalInfoResponse approval = null;
        if (result.status() == AgentStatus.APPROVAL_REQUIRED && !result.approvalId().isBlank()) {
            approval = ApprovalInfoResponse.from(approvalService.getById(result.approvalId()));
        }

        return new ChatResponse(
                ApiContract.VERSION,
                result.status().name().toLowerCase(),
                result.message(),
                stepDetails,
                result.approvalId(),
                CorrelationIdHolder.getOrCreate(),
                approval
        );
    }
}
