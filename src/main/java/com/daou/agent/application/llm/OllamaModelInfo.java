package com.daou.agent.application.llm;

public record OllamaModelInfo(
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
