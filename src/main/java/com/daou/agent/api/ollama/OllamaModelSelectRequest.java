package com.daou.agent.api.ollama;

import jakarta.validation.constraints.NotBlank;

public record OllamaModelSelectRequest(
        @NotBlank String sessionId,
        @NotBlank String model
) {
}
