package com.daou.agent.api.common;

public record ErrorResponse(
        String version,
        String status,
        String errorCode,
        String message,
        String correlationId
) {
}
