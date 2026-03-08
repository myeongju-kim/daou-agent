package com.daou.agent.api.ollama;

import com.daou.agent.application.llm.OllamaModelInfo;
import com.daou.agent.application.llm.OllamaModelService;
import com.daou.agent.application.llm.OllamaModelSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
