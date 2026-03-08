package com.daou.agent.api.ollama;

public record OllamaModelSelectResponse(
        String provider,
        String sessionId,
        String model,
        String message
) {
}
