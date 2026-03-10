package com.daou.agent.application.llm;

import java.util.List;

public record OllamaModelSummary(
        String provider,
        int modelCount,
        List<OllamaModelInfo> models
) {
    public OllamaModelSummary {
        models = models == null ? List.of() : List.copyOf(models);
    }
}
