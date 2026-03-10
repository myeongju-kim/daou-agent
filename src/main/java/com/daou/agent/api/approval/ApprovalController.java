package com.daou.agent.api.approval;

import com.daou.agent.api.chat.ChatResponse;
import com.daou.agent.api.common.ApiContract;
import com.daou.agent.application.approval.ApprovalResumeService;
import com.daou.agent.application.approval.ApprovalService;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.LoopStep;
import com.daou.agent.domain.common.ApprovalStatus;
import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApprovalController {

    private final ApprovalService approvalService;
    private final ApprovalResumeService approvalResumeService;

    public ApprovalController(ApprovalService approvalService, ApprovalResumeService approvalResumeService) {
        this.approvalService = approvalService;
        this.approvalResumeService = approvalResumeService;
    }

    @PostMapping("/approvals/{id}/approve")
    public ApprovalResponse approve(@PathVariable("id") String approvalId) {
        ApprovalStatus status = approvalService.approve(approvalId);
        return new ApprovalResponse(
                ApiContract.VERSION,
                approvalId,
                status.name().toLowerCase(),
                "승인 처리되었습니다.",
                CorrelationIdHolder.getOrCreate()
        );
    }

    @PostMapping("/approvals/{id}/reject")
    public ApprovalResponse reject(@PathVariable("id") String approvalId) {
        ApprovalStatus status = approvalService.reject(approvalId);
        return new ApprovalResponse(
                ApiContract.VERSION,
                approvalId,
                status.name().toLowerCase(),
                "거절 처리되었습니다.",
                CorrelationIdHolder.getOrCreate()
        );
    }

    @PostMapping("/approvals/{id}/approve-and-resume")
    public ChatResponse approveAndResume(@PathVariable("id") String approvalId) {
        AgentResult result = approvalResumeService.approveAndResume(approvalId);
        List<String> stepDetails = result.steps().stream()
                .map(LoopStep::detail)
                .toList();
        return new ChatResponse(
                ApiContract.VERSION,
                result.status().name().toLowerCase(),
                result.message(),
                stepDetails,
                result.approvalId(),
                CorrelationIdHolder.getOrCreate()
        );
    }
}
