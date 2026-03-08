package com.daou.agent.application.llm;

import com.daou.agent.application.session.SessionService;
import java.util.List;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OllamaModelService {

    private final ObjectProvider<OllamaApi> ollamaApiProvider;
    private final String llmProvider;
    private final SessionService sessionService;

    public OllamaModelService(
            ObjectProvider<OllamaApi> ollamaApiProvider,
            @Value("${agent.llm-provider:ollama}") String llmProvider,
            SessionService sessionService
    ) {
        this.ollamaApiProvider = ollamaApiProvider;
        this.llmProvider = llmProvider;
        this.sessionService = sessionService;
    }

    public OllamaModelSummary getModels() {
        OllamaApi.ListModelResponse response = requireOllamaApi().listModels();
        List<OllamaModelInfo> models = response.models() == null ? List.of() : response.models().stream()
                .map(this::toModelInfo)
                .toList();
        return new OllamaModelSummary("ollama", models.size(), models);
    }

    public String selectModel(String sessionId, String model) {
        String normalizedModel = model == null ? "" : model.trim();
        if (normalizedModel.isBlank()) {
            throw new IllegalArgumentException("model 값은 비어 있을 수 없습니다.");
        }

        boolean exists = getModels().models().stream()
                .anyMatch(info -> normalizedModel.equals(info.name()) || normalizedModel.equals(info.model()));

        if (!exists) {
            throw new IllegalArgumentException("Ollama에 등록되지 않은 모델입니다: " + normalizedModel);
        }

        sessionService.setSelectedModel(sessionId, normalizedModel);
        return normalizedModel;
    }

    public String getSelectedModel(String sessionId) {
        return sessionService.getSelectedModel(sessionId);
    }

    public String ensureSelectedModel(String sessionId) {
        String selected = sessionService.getSelectedModel(sessionId);
        if (selected != null && !selected.isBlank()) {
            return selected;
        }

        OllamaModelSummary summary = getModels();
        OllamaModelInfo first = summary.models().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Ollama에 설치된 모델이 없습니다."));

        String fallback = first.model().isBlank() ? first.name() : first.model();
        if (fallback.isBlank()) {
            throw new IllegalStateException("Ollama 모델명 확인에 실패했습니다.");
        }

        sessionService.setSelectedModel(sessionId, fallback);
        return fallback;
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

    private OllamaApi requireOllamaApi() {
        if (!"ollama".equalsIgnoreCase(llmProvider)) {
            throw new IllegalArgumentException("현재 llm-provider가 ollama가 아닙니다: " + llmProvider);
        }
        OllamaApi ollamaApi = ollamaApiProvider.getIfAvailable();
        if (ollamaApi == null) {
            throw new IllegalArgumentException("Ollama API bean을 찾을 수 없습니다. 설정을 확인해주세요.");
        }
        return ollamaApi;
    }
}
