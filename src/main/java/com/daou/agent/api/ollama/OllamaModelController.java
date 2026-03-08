package com.daou.agent.api.ollama;

import com.daou.agent.application.llm.OllamaModelInfo;
import com.daou.agent.application.llm.OllamaModelService;
import com.daou.agent.application.llm.OllamaModelSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OllamaModelController {

    private final OllamaModelService ollamaModelService;

    public OllamaModelController(OllamaModelService ollamaModelService) {
        this.ollamaModelService = ollamaModelService;
    }

    @GetMapping("/ollama/models")
    public OllamaModelsResponse models() {
        OllamaModelSummary summary = ollamaModelService.getModels();
        List<OllamaModelResponse> models = summary.models().stream()
                .map(this::toResponse)
                .toList();
        return new OllamaModelsResponse(summary.provider(), summary.modelCount(), models);
    }

    @PostMapping("/ollama/models/select")
    public OllamaModelSelectResponse selectModel(@Valid @RequestBody OllamaModelSelectRequest request) {
        String selected = ollamaModelService.selectModel(request.sessionId(), request.model());
        return new OllamaModelSelectResponse(
                "ollama",
                request.sessionId(),
                selected,
                "선택한 모델이 세션에 저장되었습니다."
        );
    }

    @GetMapping("/ollama/models/select/{sessionId}")
    public OllamaModelSelectResponse selectedModel(@PathVariable("sessionId") String sessionId) {
        String selected = ollamaModelService.getSelectedModel(sessionId);
        return new OllamaModelSelectResponse(
                "ollama",
                sessionId,
                selected,
                selected.isBlank() ? "저장된 모델이 없습니다." : "현재 세션의 선택 모델입니다."
        );
    }

    private OllamaModelResponse toResponse(OllamaModelInfo info) {
        return new OllamaModelResponse(
                info.name(),
                info.model(),
                info.modifiedAt(),
                info.size(),
                info.digest(),
                info.family(),
                info.parameterSize(),
                info.quantizationLevel()
        );
    }
}
