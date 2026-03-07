package com.daou.agent.application.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.daou.agent.domain.approval.ApprovalDecision;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolDefinition;
import com.daou.agent.domain.tool.ToolRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApprovalPolicyTest {

    private final ApprovalPolicy approvalPolicy = new ApprovalPolicy(new ToolRegistry(List.of(
            new ToolDefinition("calendar.list_events", "", RiskLevel.LOW),
            new ToolDefinition("calendar.create_event", "", RiskLevel.MEDIUM),
            new ToolDefinition("fs.read", "", RiskLevel.HIGH)
    )));

    @Test
    void shouldAutoApproveForLowRiskTool() {
        ApprovalDecision decision = approvalPolicy.check(new ToolCallRequest("calendar.list_events", Map.of()));
        assertThat(decision.requiresApproval()).isFalse();
        assertThat(decision.blocked()).isFalse();
    }

    @Test
    void shouldRequireApprovalForMediumRiskTool() {
        ApprovalDecision decision = approvalPolicy.check(new ToolCallRequest("calendar.create_event", Map.of()));
        assertThat(decision.requiresApproval()).isTrue();
        assertThat(decision.blocked()).isFalse();
    }

    @Test
    void shouldBlockHighRiskTool() {
        ApprovalDecision decision = approvalPolicy.check(new ToolCallRequest("fs.read", Map.of()));
        assertThat(decision.blocked()).isTrue();
    }
}
