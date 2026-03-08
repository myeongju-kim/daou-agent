package com.daou.agent.infrastructure.llm;

import com.daou.agent.application.agent.LlmResponse;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LlmJsonResponseParser {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public LlmJsonResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LlmResponse parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LlmResponse.finalAnswer("LLM 응답이 비어 있습니다.");
        }

        JsonNode root = tryParseJsonObject(raw);
        if (root == null || !root.isObject()) {
            return LlmResponse.finalAnswer(raw.trim());
        }

        String type = root.path("type").asText("final").trim().toLowerCase();
        if ("tool_call".equals(type)) {
            String toolName = root.path("toolName").asText("").trim();
            if (!toolName.isBlank()) {
                Map<String, Object> arguments = toArguments(root.path("arguments"));
                return LlmResponse.toolCall(new ToolCallRequest(toolName, arguments));
            }
        }

        String message = root.path("message").asText("").trim();
        if (!message.isBlank()) {
            return LlmResponse.finalAnswer(message);
        }
        return LlmResponse.finalAnswer(raw.trim());
    }

    private Map<String, Object> toArguments(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        if (!node.isObject()) {
            return Map.of();
        }
        return objectMapper.convertValue(node, MAP_TYPE);
    }

    private JsonNode tryParseJsonObject(String raw) {
        String trimmed = raw.trim();
        String candidate = extractJsonCandidate(trimmed);
        try {
            return objectMapper.readTree(candidate);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractJsonCandidate(String text) {
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1);
        }
        return text;
    }
}
