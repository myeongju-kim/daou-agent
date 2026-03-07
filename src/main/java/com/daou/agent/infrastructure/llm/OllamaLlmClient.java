package com.daou.agent.infrastructure.llm;

import com.daou.agent.application.agent.LlmResponse;
import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.agent.AgentContext;

public class OllamaLlmClient implements LlmClient {

    @Override
    public LlmResponse generate(AgentContext context) {
        return OpenAiLlmClient.decide(context, "ollama");
    }
}
