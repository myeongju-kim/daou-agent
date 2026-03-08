package com.daou.agent.domain.approval;

public record ApprovalDecision(boolean requiresApproval, boolean blocked, String message) {

    public static ApprovalDecision autoApproved() {
        return new ApprovalDecision(false, false, "auto approved");
    }

    public static ApprovalDecision requiresApproval(String message) {
        return new ApprovalDecision(true, false, message);
    }

    public static ApprovalDecision blocked(String message) {
        return new ApprovalDecision(false, true, message);
    }
}
