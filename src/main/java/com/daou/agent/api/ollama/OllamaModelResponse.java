package com.daou.agent.api.ollama;

public record OllamaModelResponse(
        String name,
        String model,
        String modifiedAt,
        long size,
        String digest,
        String family,
        String parameterSize,
        String quantizationLevel
) {
}
