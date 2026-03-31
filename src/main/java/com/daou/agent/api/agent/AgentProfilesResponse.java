package com.daou.agent.api.agent;

import java.util.List;

public record AgentProfilesResponse(
        String version,
        String defaultAgentKey,
        List<AgentProfileItemResponse> agents,
        String correlationId
) {
}
