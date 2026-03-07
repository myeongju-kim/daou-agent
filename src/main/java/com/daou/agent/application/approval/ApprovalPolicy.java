package com.daou.agent.application.approval;

import com.daou.agent.domain.approval.ApprovalDecision;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolDefinition;
import com.daou.agent.domain.tool.ToolRegistry;
import org.springframework.stereotype.Component;

@Component
public class ApprovalPolicy {

    private final ToolRegistry toolRegistry;

    public ApprovalPolicy(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    public ApprovalDecision check(ToolCallRequest toolCall) {
        ToolDefinition definition = toolRegistry.find(toolCall.toolName())
                .orElse(null);

        if (definition == null) {
            return ApprovalDecision.blocked("등록되지 않은 도구입니다: " + toolCall.toolName());
        }

        RiskLevel riskLevel = definition.riskLevel();
        if (riskLevel == RiskLevel.HIGH) {
            return ApprovalDecision.blocked("HIGH 위험 도구는 기본 차단됩니다: " + toolCall.toolName());
        }
        if (riskLevel == RiskLevel.MEDIUM) {
            return ApprovalDecision.requiresApproval("승인이 필요한 도구입니다: " + toolCall.toolName());
        }
        return ApprovalDecision.autoApproved();
    }
}
