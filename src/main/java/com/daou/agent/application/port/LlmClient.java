package com.daou.agent.application.port;

import com.daou.agent.application.agent.LlmResponse;
import com.daou.agent.domain.agent.AgentContext;

public interface LlmClient {
    LlmResponse generate(AgentContext context);
}
