package com.daou.agent.api.approval;

import com.daou.agent.application.approval.ApprovalService;
import com.daou.agent.domain.common.ApprovalStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/approvals/{id}/approve")
    public ApprovalResponse approve(@PathVariable("id") String approvalId) {
        ApprovalStatus status = approvalService.approve(approvalId);
        return new ApprovalResponse(approvalId, status.name().toLowerCase(), "승인 처리되었습니다.");
    }

    @PostMapping("/approvals/{id}/reject")
    public ApprovalResponse reject(@PathVariable("id") String approvalId) {
        ApprovalStatus status = approvalService.reject(approvalId);
        return new ApprovalResponse(approvalId, status.name().toLowerCase(), "거절 처리되었습니다.");
    }
}
