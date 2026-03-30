package com.daou.agent.application.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.daou.agent.application.approval.ApprovalPolicy;
import com.daou.agent.application.approval.ApprovalService;
import com.daou.agent.application.port.LlmClient;
import com.daou.agent.application.port.ToolExecutor;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.AgentStatus;
import com.daou.agent.domain.approval.ApprovalRequest;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolDefinition;
import com.daou.agent.domain.tool.ToolRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AgentLoopEngineTest {

    @Test
    void shouldRetryWithForcedToolCallForExecutionRequest() {
        LlmClient llmClient = mock(LlmClient.class);
        ToolExecutor toolExecutor = mock(ToolExecutor.class);
        ApprovalService approvalService = mock(ApprovalService.class);

        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new ToolDefinition("mail.send_message", "메일 발송", RiskLevel.MEDIUM)
        ));
        ApprovalPolicy approvalPolicy = new ApprovalPolicy(toolRegistry);
        AgentLoopEngine engine = new AgentLoopEngine(
                llmClient,
                toolExecutor,
                approvalPolicy,
                approvalService,
                toolRegistry,
                5
        );

        ToolCallRequest toolCallRequest = new ToolCallRequest("mail.send_message", Map.of(
                "to", "kingmj@daou.co.kr",
                "subject", "일정 브리핑",
                "content", "내용"
        ));
        when(llmClient.generate(any(AgentContext.class)))
                .thenReturn(LlmResponse.finalAnswer("메일을 발송하겠습니다."))
                .thenReturn(LlmResponse.toolCall(toolCallRequest));
        when(approvalService.create(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ApprovalRequest(
                        "default",
                        toolCallRequest,
                        RiskLevel.MEDIUM,
                        "요청",
                        "",
                        "",
                        "corr"
                ));

        AgentResult result = engine.execute(new AgentContext(
                "default",
                "",
                List.of(),
                "kingmj@daou.co.kr 로 메일 발송해줘",
                ""
        ));

        assertThat(result.status()).isEqualTo(AgentStatus.APPROVAL_REQUIRED);
        assertThat(result.steps()).extracting("type").contains("retry", "approval_required");
    }

    @Test
    void shouldReturnErrorWhenToolCallMissingAfterForcedRetry() {
        LlmClient llmClient = mock(LlmClient.class);
        ToolExecutor toolExecutor = mock(ToolExecutor.class);
        ApprovalService approvalService = mock(ApprovalService.class);

        ToolRegistry toolRegistry = new ToolRegistry(List.of(
                new ToolDefinition("mail.send_message", "메일 발송", RiskLevel.MEDIUM)
        ));
        ApprovalPolicy approvalPolicy = new ApprovalPolicy(toolRegistry);
        AgentLoopEngine engine = new AgentLoopEngine(
                llmClient,
                toolExecutor,
                approvalPolicy,
                approvalService,
                toolRegistry,
                5
        );

        when(llmClient.generate(any(AgentContext.class)))
                .thenReturn(LlmResponse.finalAnswer("메일을 발송하겠습니다."))
                .thenReturn(LlmResponse.finalAnswer("곧 발송하겠습니다."));

        AgentResult result = engine.execute(new AgentContext(
                "default",
                "",
                List.of(),
                "메일 발송해줘",
                ""
        ));

        assertThat(result.status()).isEqualTo(AgentStatus.ERROR);
        assertThat(result.message()).contains("도구 호출");
        assertThat(result.steps()).extracting("type").contains("retry", "error");
    }
}

