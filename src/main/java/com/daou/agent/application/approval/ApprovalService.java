package com.daou.agent.application.approval;

import com.daou.agent.application.port.ApprovalRepository;
import com.daou.agent.domain.approval.ApprovalRequest;
import com.daou.agent.domain.common.ApprovalStatus;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final ApprovalRepository approvalRepository;

    public ApprovalService(ApprovalRepository approvalRepository) {
        this.approvalRepository = approvalRepository;
    }

    public ApprovalRequest create(
            String sessionId,
            ToolCallRequest toolCall,
            RiskLevel riskLevel,
            String requestedUserMessage,
            String summarySnapshot,
            String selectedModelSnapshot
    ) {
        ApprovalRequest approvalRequest = new ApprovalRequest(
                sessionId,
                toolCall,
                riskLevel,
                requestedUserMessage,
                summarySnapshot,
                selectedModelSnapshot,
                CorrelationIdHolder.getOrCreate()
        );
        log.info("event=approval.create approvalId={} sessionId={} toolName={} riskLevel={}",
                approvalRequest.getId(), sessionId, toolCall.toolName(), riskLevel.name());
        return approvalRepository.save(approvalRequest);
    }

    public ApprovalStatus approve(String approvalId) {
        ApprovalRequest request = getById(approvalId);
        ApprovalStatus status = request.approve();
        approvalRepository.save(request);
        log.info("event=approval.approve approvalId={} status={}", approvalId, status.name());
        return status;
    }

    public ApprovalRequest approveAndGet(String approvalId) {
        ApprovalRequest request = getById(approvalId);
        request.approve();
        approvalRepository.save(request);
        return request;
    }

    public ApprovalStatus reject(String approvalId) {
        ApprovalRequest request = getById(approvalId);
        ApprovalStatus status = request.reject();
        approvalRepository.save(request);
        log.info("event=approval.reject approvalId={} status={}", approvalId, status.name());
        return status;
    }

    public ApprovalRequest getById(String approvalId) {
        return approvalRepository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("approval request not found: " + approvalId));
    }

    public long countPending() {
        return approvalRepository.countPending();
    }
}
