package com.daou.agent.application.approval;

import com.daou.agent.application.port.ApprovalRepository;
import com.daou.agent.domain.approval.ApprovalRequest;
import com.daou.agent.domain.common.ApprovalStatus;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolCallRequest;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService {

    private final ApprovalRepository approvalRepository;

    public ApprovalService(ApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    public ApprovalRequest create(String sessionId, ToolCallRequest toolCall, RiskLevel riskLevel) {
        return approvalRepository.save(new ApprovalRequest(sessionId, toolCall, riskLevel));
    }

    public ApprovalStatus approve(String approvalId) {
        ApprovalRequest request = getById(approvalId);
        request.approve();
        return request.getStatus();
    }

    public ApprovalRequest approveAndGet(String approvalId) {
        ApprovalRequest request = getById(approvalId);
        request.approve();
        return request;
    }

    public ApprovalStatus reject(String approvalId) {
        ApprovalRequest request = getById(approvalId);
        request.reject();
        return request.getStatus();
    }

    public ApprovalRequest getById(String approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("approval request not found: " + approvalId));
    }

    public long countPending() {
        return approvalRepository.countPending();
    }
}
