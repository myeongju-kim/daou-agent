package com.daou.agent.application.agent;

import com.daou.agent.application.approval.ApprovalPolicy;
import com.daou.agent.application.approval.ApprovalService;
import com.daou.agent.application.port.LlmClient;
import com.daou.agent.application.port.ToolExecutor;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.LoopStep;
import com.daou.agent.domain.approval.ApprovalDecision;
import com.daou.agent.domain.approval.ApprovalRequest;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import com.daou.agent.domain.tool.ToolDefinition;
import com.daou.agent.domain.tool.ToolRegistry;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgentLoopEngine {

    private static final Logger log = LoggerFactory.getLogger(AgentLoopEngine.class);

    private final LlmClient llmClient;
    private final ToolExecutor toolExecutor;
    private final ApprovalPolicy approvalPolicy;
    private final ApprovalService approvalService;
    private final ToolRegistry toolRegistry;
    private final int maxLoop;

    public AgentLoopEngine(
            LlmClient llmClient,
            ToolExecutor toolExecutor,
            ApprovalPolicy approvalPolicy,
            ApprovalService approvalService,
            ToolRegistry toolRegistry,
            @Value("${agent.max-loop:5}") int maxLoop
    ) {
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.approvalPolicy = approvalPolicy;
        this.approvalService = approvalService;
        this.toolRegistry = toolRegistry;
        this.maxLoop = maxLoop;
    }

    public AgentResult execute(AgentContext context) {
        List<LoopStep> steps = new ArrayList<>();

        for (int i = 0; i < maxLoop; i++) {
            log.info("event=agent.loop.think sessionId={} loop={} selectedModel={}",
                    context.getSessionId(), i + 1, context.getSelectedModel());
            LlmResponse response = llmClient.generate(context);
            steps.add(new LoopStep("think", "loop=" + (i + 1)));

            if (response.isFinalAnswer()) {
                log.info("event=agent.loop.final sessionId={} loop={}", context.getSessionId(), i + 1);
                return AgentResult.ok(response.finalAnswer(), steps);
            }

            if (!response.hasToolCall()) {
                return AgentResult.error("LLM 응답에 final answer/tool call 이 없습니다.", steps);
            }

            ToolCallRequest toolCall = response.toolCallRequest();
            ApprovalDecision decision = approvalPolicy.check(toolCall);
            log.info("event=agent.loop.tool_decision sessionId={} toolName={} requiresApproval={} blocked={}",
                    context.getSessionId(), toolCall.toolName(), decision.requiresApproval(), decision.blocked());

            if (decision.blocked()) {
                steps.add(new LoopStep("blocked", decision.message()));
                return AgentResult.blocked(decision.message(), steps);
            }

            if (decision.requiresApproval()) {
                ToolDefinition definition = toolRegistry.find(toolCall.toolName())
                        .orElseThrow(() -> new IllegalArgumentException("tool not found: " + toolCall.toolName()));

                ApprovalRequest approvalRequest = approvalService.create(
                        context.getSessionId(),
                        toolCall,
                        definition.riskLevel(),
                        context.getCurrentUserMessage(),
                        context.getSummary(),
                        context.getSelectedModel()
                );
                steps.add(new LoopStep("approval_required", toolCall.toolName()));
                return AgentResult.approvalRequired(
                        decision.message(),
                        approvalRequest.getId(),
                        steps
                );
            }

            ToolCallResult toolResult = toolExecutor.execute(toolCall);
            context.addToolResult(toolResult);
            steps.add(new LoopStep("tool_result", toolResult.toolName() + ": " + toolResult.message()));
            log.info("event=agent.loop.tool_result sessionId={} toolName={} status={}",
                    context.getSessionId(), toolResult.toolName(), toolResult.status());
        }

        steps.add(new LoopStep("error", "max loop reached"));
        return AgentResult.error("최대 루프 횟수에 도달했습니다.", steps);
    }
}
