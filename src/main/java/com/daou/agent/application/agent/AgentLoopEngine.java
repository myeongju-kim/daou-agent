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
import java.util.Locale;
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
                if (shouldForceToolCall(context)) {
                    LlmResponse forcedResponse = forceToolCallRetry(context);
                    steps.add(new LoopStep("retry", "force_tool_call_for_action"));
                    if (forcedResponse.hasToolCall()) {
                        response = forcedResponse;
                    } else {
                        log.warn("event=agent.loop.final_without_tool_call sessionId={} loop={} message={}",
                                context.getSessionId(), i + 1, response.finalAnswer());
                        steps.add(new LoopStep("error", "tool_call_missing_for_action"));
                        return AgentResult.error("실행 요청으로 판단됐지만 도구 호출을 생성하지 못했습니다. 다시 시도해 주세요.", steps);
                    }
                } else {
                log.info("event=agent.loop.final sessionId={} loop={}", context.getSessionId(), i + 1);
                return AgentResult.ok(response.finalAnswer(), steps);
                }
            }

            if (!response.hasToolCall()) {
                return AgentResult.error("LLM 응답에 final answer/tool call 이 없습니다.", steps);
            }

            ToolCallRequest toolCall = response.toolCallRequest();
            if (!context.isToolAllowed(toolCall.toolName())) {
                String message = "선택한 에이전트에서 허용되지 않은 도구입니다: " + toolCall.toolName();
                steps.add(new LoopStep("blocked", message));
                log.warn("event=agent.loop.tool_forbidden sessionId={} agentKey={} toolName={}",
                        context.getSessionId(), context.getAgentKey(), toolCall.toolName());
                return AgentResult.blocked(message, steps);
            }

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

    private boolean shouldForceToolCall(AgentContext context) {
        if (!context.getToolResults().isEmpty()) {
            return false;
        }
        String message = context.getCurrentUserMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        boolean hasActionVerb = containsAny(lower,
                "보내", "발송", "전송", "등록", "생성", "추가", "예약", "삭제", "수정", "작성",
                "send", "create", "register", "add", "schedule", "delete", "update");
        boolean hasDomain = containsAny(lower, "메일", "email", "일정", "calendar", "캘린더", "메신저", "messenger");
        return hasActionVerb && hasDomain;
    }

    private LlmResponse forceToolCallRetry(AgentContext context) {
        String forcedMessage = """
                %s

                [시스템 강제 규칙]
                - 현재 요청은 실행형 요청이다.
                - final 응답을 금지한다.
                - 반드시 {"type":"tool_call","toolName":"...","arguments":{...}} 형식으로만 응답한다.
                """.formatted(context.getCurrentUserMessage());
        AgentContext retryContext = new AgentContext(
                context.getSessionId(),
                context.getSummary(),
                context.getRecentMessages(),
                forcedMessage,
                context.getSelectedModel(),
                context.getAgentKey(),
                context.getIntent(),
                context.getAgentSystemHint(),
                context.getAllowedToolNames()
        );
        context.getToolResults().forEach(retryContext::addToolResult);
        return llmClient.generate(retryContext);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
