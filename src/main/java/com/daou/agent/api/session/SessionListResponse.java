package com.daou.agent.api.session;

import java.util.List;

public record SessionListResponse(
        String version,
        String agentKey,
        List<SessionListItemResponse> sessions,
        String correlationId
) {
}
