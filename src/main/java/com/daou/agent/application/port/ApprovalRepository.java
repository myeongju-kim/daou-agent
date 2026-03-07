package com.daou.agent.application.port;

import com.daou.agent.domain.approval.ApprovalRequest;
import java.util.Optional;

public interface ApprovalRepository {
    ApprovalRequest save(ApprovalRequest request);

    Optional<ApprovalRequest> findById(String id);

    long countPending();
}
