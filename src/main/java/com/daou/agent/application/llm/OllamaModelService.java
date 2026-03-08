package com.daou.agent.application.llm;

import java.util.List;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OllamaModelService {

    private final ObjectProvider<OllamaApi> ollamaApiProvider;
    private final String llmProvider;

    public OllamaModelService(
            ObjectProvider<OllamaApi> ollamaApiProvider,
            @Value("${agent.llm-provider:ollama}") String llmProvider
    ) {
        this.ollamaApiProvider = ollamaApiProvider;
        this.llmProvider = llmProvider;
    }

    public OllamaModelSummary getModels() {
        if (!"ollama".equalsIgnoreCase(llmProvider)) {
            throw new IllegalArgumentException("현재 llm-provider가 ollama가 아닙니다: " + llmProvider);
        }

        OllamaApi ollamaApi = ollamaApiProvider.getIfAvailable();
        if (ollamaApi == null) {
            throw new IllegalArgumentException("Ollama API bean을 찾을 수 없습니다. 설정을 확인해주세요.");
        }

        OllamaApi.ListModelResponse response = ollamaApi.listModels();
        List<OllamaModelInfo> models = response.models() == null ? List.of() : response.models().stream()
                .map(this::toModelInfo)
                .toList();
        return new OllamaModelSummary("ollama", models.size(), models);
    }

    private OllamaModelInfo toModelInfo(OllamaApi.Model model) {
        String modifiedAt = model.modifiedAt() == null ? "" : model.modifiedAt().toString();
        String family = model.details() == null ? "" : emptyToBlank(model.details().family());
        String parameterSize = model.details() == null ? "" : emptyToBlank(model.details().parameterSize());
        String quantizationLevel = model.details() == null ? "" : emptyToBlank(model.details().quantizationLevel());

        return new OllamaModelInfo(
                emptyToBlank(model.name()),
                emptyToBlank(model.model()),
                modifiedAt,
                model.size() == null ? 0L : model.size(),
                emptyToBlank(model.digest()),
                family,
                parameterSize,
                quantizationLevel
        );
    }

    private String emptyToBlank(String value) {
        return value == null ? "" : value;
    }
}
