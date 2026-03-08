package com.daou.agent.domain.agent;

import java.util.List;

public record AgentResult(
        AgentStatus status,
        String message,
        List<LoopStep> steps,
        String approvalId
) {
    public AgentResult {
        steps = steps == null ? List.of() : List.copyOf(steps);
        message = message == null ? "" : message;
        approvalId = approvalId == null ? "" : approvalId;
    }

    public static AgentResult ok(String message, List<LoopStep> steps) {
        return new AgentResult(AgentStatus.OK, message, steps, "");
    }

    public static AgentResult approvalRequired(String message, String approvalId, List<LoopStep> steps) {
        return new AgentResult(AgentStatus.APPROVAL_REQUIRED, message, steps, approvalId);
    }

    public static AgentResult blocked(String message, List<LoopStep> steps) {
        return new AgentResult(AgentStatus.BLOCKED, message, steps, "");
    }

    public static AgentResult error(String message, List<LoopStep> steps) {
        return new AgentResult(AgentStatus.ERROR, message, steps, "");
    }
}
