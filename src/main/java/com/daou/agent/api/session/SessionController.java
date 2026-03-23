package com.daou.agent.api.session;

import com.daou.agent.api.common.ApiContract;
import com.daou.agent.application.session.SessionDetailView;
import com.daou.agent.application.session.SessionQueryService;
import com.daou.agent.application.session.SessionSummaryView;
import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    private final SessionQueryService sessionQueryService;

    public SessionController(SessionQueryService sessionQueryService) {
        this.sessionQueryService = sessionQueryService;
    }

    @GetMapping("/sessions")
    public SessionListResponse listSessions(
            @RequestParam(name = "agentKey", required = false) String agentKey,
            @RequestParam(name = "limit", defaultValue = "20") int limit
    ) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 100);
        List<SessionListItemResponse> sessions = sessionQueryService.getSessions(agentKey, normalizedLimit).stream()
                .map(this::toListItem)
                .toList();

        return new SessionListResponse(
                ApiContract.VERSION,
                normalizeAgentKey(agentKey),
                sessions,
                CorrelationIdHolder.getOrCreate()
        );
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionDetailResponse getSession(
            @PathVariable String sessionId,
            @RequestParam(name = "agentKey", required = false) String agentKey
    ) {
        SessionDetailView session = sessionQueryService.getSession(agentKey, sessionId);
        return new SessionDetailResponse(
                ApiContract.VERSION,
                session.sessionId(),
                session.agentKey(),
                session.title(),
                session.summary(),
                session.selectedModel(),
                session.createdAt(),
                session.updatedAt(),
                session.messages().stream()
                        .map(message -> new SessionMessageResponse(
                                message.role().name().toLowerCase(),
                                message.type().name().toLowerCase(),
                                message.content(),
                                message.createdAt()
                        ))
                        .toList(),
                CorrelationIdHolder.getOrCreate()
        );
    }

    private SessionListItemResponse toListItem(SessionSummaryView session) {
        return new SessionListItemResponse(
                session.sessionId(),
                session.agentKey(),
                session.title(),
                session.summary(),
                session.selectedModel(),
                session.lastMessage(),
                session.messageCount(),
                session.createdAt(),
                session.updatedAt()
        );
    }

    private String normalizeAgentKey(String agentKey) {
        if (agentKey == null || agentKey.isBlank()) {
            return "daouoffice";
        }
        return agentKey.trim();
    }
}
