package com.daou.agent.infrastructure.persistence;

import com.daou.agent.application.port.ApprovalRepository;
import com.daou.agent.domain.approval.ApprovalRequest;
import com.daou.agent.domain.common.ApprovalStatus;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "agent.storage.type", havingValue = "memory")
public class InMemoryApprovalRepository implements ApprovalRepository {

    private final Map<String, ApprovalRequest> store = new ConcurrentHashMap<>();

    @Override
    public ApprovalRequest save(ApprovalRequest request) {
        store.put(request.getId(), request);
        return request;
    }

    @Override
    public Optional<ApprovalRequest> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public long countPending() {
        return store.values().stream()
                .filter(req -> req.getStatus() == ApprovalStatus.PENDING)
                .count();
    }
}
