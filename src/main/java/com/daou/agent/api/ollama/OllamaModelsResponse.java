package com.daou.agent.api.ollama;

import java.util.List;

public record OllamaModelsResponse(
        String provider,
        int modelCount,
        List<OllamaModelResponse> models
) {
    public OllamaModelsResponse {
        models = models == null ? List.of() : List.copyOf(models);
    }
}
