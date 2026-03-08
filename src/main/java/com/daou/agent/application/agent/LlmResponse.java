package com.daou.agent.application.agent;

import com.daou.agent.domain.tool.ToolCallRequest;

public record LlmResponse(String finalAnswer, ToolCallRequest toolCallRequest) {

    public static LlmResponse finalAnswer(String answer) {
        return new LlmResponse(answer, null);
    }

    public static LlmResponse toolCall(ToolCallRequest request) {
        return new LlmResponse(null, request);
    }

    public boolean isFinalAnswer() {
        return finalAnswer != null && !finalAnswer.isBlank();
    }

    public boolean hasToolCall() {
        return toolCallRequest != null;
    }
}
