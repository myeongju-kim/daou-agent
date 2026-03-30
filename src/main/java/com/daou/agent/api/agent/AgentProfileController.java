package com.daou.agent.api.agent;

import com.daou.agent.api.common.ApiContract;
import com.daou.agent.application.agent.AgentProfileService;
import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentProfileController {

    private final AgentProfileService agentProfileService;

    public AgentProfileController(AgentProfileService agentProfileService) {
        this.agentProfileService = agentProfileService;
    }

    @GetMapping("/agents")
    public AgentProfilesResponse listAgents() {
        String defaultAgentKey = agentProfileService.defaultAgentKey();
        return new AgentProfilesResponse(
                ApiContract.VERSION,
                defaultAgentKey,
                agentProfileService.listProfiles().stream()
                        .map(profile -> new AgentProfileItemResponse(
                                profile.key(),
                                profile.name(),
                                profile.description(),
                                profile.key().equals(defaultAgentKey),
                                profile.supportedIntents(),
                                profile.allowedTools()
                        ))
                        .toList(),
                CorrelationIdHolder.getOrCreate()
        );
    }
}
