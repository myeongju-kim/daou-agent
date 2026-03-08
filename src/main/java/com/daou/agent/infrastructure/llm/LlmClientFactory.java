package com.daou.agent.infrastructure.llm;

import com.daou.agent.application.port.LlmClient;
import org.springframework.stereotype.Component;

@Component
public class LlmClientFactory {

    public LlmClient create(String provider) {
        if (provider == null) {
            return new OllamaLlmClient();
        }
        return switch (provider.toLowerCase()) {
            case "openai" -> new OpenAiLlmClient();
            case "ollama" -> new OllamaLlmClient();
            default -> throw new IllegalArgumentException("unsupported llm provider: " + provider);
        };
    }
}
