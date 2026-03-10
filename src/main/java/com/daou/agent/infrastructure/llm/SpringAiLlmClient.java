package com.daou.agent.infrastructure.llm;

import com.daou.agent.application.agent.LlmResponse;
import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.session.SessionMessage;
import com.daou.agent.domain.tool.ToolCallResult;
import com.daou.agent.domain.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.client.RestClient;

public class SpringAiLlmClient implements LlmClient {

    private final String provider;
    private final ToolRegistry toolRegistry;
    private final LlmJsonResponseParser responseParser;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final RestClient ollamaRestClient;
    private final String defaultOllamaModel;

    public SpringAiLlmClient(
            String provider,
            ChatModel chatModel,
            ToolRegistry toolRegistry,
            LlmJsonResponseParser responseParser,
            ObjectMapper objectMapper,
            String ollamaBaseUrl,
            String defaultOllamaModel
    ) {
        this.provider = provider;
        this.toolRegistry = toolRegistry;
        this.responseParser = responseParser;
        this.objectMapper = objectMapper;
        this.chatClient = ChatClient.create(chatModel);
        this.ollamaRestClient = RestClient.builder().baseUrl(ollamaBaseUrl).build();
        this.defaultOllamaModel = defaultOllamaModel == null ? "" : defaultOllamaModel.trim();
    }

    @Override
    public LlmResponse generate(AgentContext context) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(context);

        try {
            String selectedModel = context.getSelectedModel();

            // Spring AI ThinkOption 직렬화와 서버 버전 호환 이슈를 피하기 위해
            // Ollama는 REST로 think=false(boolean)를 직접 보낸다.
            if ("ollama".equalsIgnoreCase(provider)) {
                String model = selectedModel.isBlank() ? defaultOllamaModel : selectedModel;
                if (!model.isBlank()) {
                    String raw = callOllamaNoThink(systemPrompt, userPrompt, model);
                    return responseParser.parse(raw);
                }
            }

            ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt);

            String raw = requestSpec.call().content();
            return responseParser.parse(raw);
        } catch (Exception e) {
            String modelInfo = context.getSelectedModel().isBlank() ? "(default)" : context.getSelectedModel();
            return LlmResponse.finalAnswer("[" + provider + "] LLM 호출 실패(model=" + modelInfo + "): " + e.getMessage());
        }
    }

    private String callOllamaNoThink(String systemPrompt, String userPrompt, String model) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("stream", false);
        requestBody.put("think", false);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        Object response = ollamaRestClient.post()
                .uri("/api/chat")
                .body(requestBody)
                .retrieve()
                .body(Object.class);

        JsonNode root = objectMapper.valueToTree(response);
        String content = root.path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("Ollama 응답에서 message.content를 찾지 못했습니다.");
        }
        return content;
    }

    private String buildSystemPrompt() {
        String tools = toolRegistry.names().stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));

        return """
                당신은 DaouOffice Agent 백엔드의 의사결정 LLM이다.
                응답은 반드시 JSON object 한 개로만 반환한다.
                설명 문장, 코드블록, 마크다운은 금지한다.

                사용 가능한 도구:
                %s

                JSON 스키마:
                1) 최종 답변
                {"type":"final","message":"사용자에게 보여줄 최종 답변"}

                2) 도구 호출
                {"type":"tool_call","toolName":"도구명","arguments":{"key":"value"}}

                규칙:
                - toolName은 반드시 사용 가능한 도구 목록 중 하나여야 한다.
                - 이미 toolResults가 존재하면 기본적으로 final을 반환한다.
                - 알 수 없는 값은 임의 생성하지 말고 final로 설명한다.
                """.formatted(tools);
    }

    private String buildUserPrompt(AgentContext context) {
        String recentMessages = context.getRecentMessages().stream()
                .map(this::formatMessage)
                .collect(Collectors.joining("\n"));

        String toolResults = context.getToolResults().stream()
                .map(this::formatToolResult)
                .collect(Collectors.joining("\n"));

        return """
                sessionId: %s
                summary: %s
                selectedModel: %s
                currentUserMessage: %s

                recentMessages:
                %s

                toolResults:
                %s
                """.formatted(
                context.getSessionId(),
                emptyToDash(context.getSummary()),
                emptyToDash(context.getSelectedModel()),
                emptyToDash(context.getCurrentUserMessage()),
                emptyToDash(recentMessages),
                emptyToDash(toolResults)
        );
    }

    private String formatMessage(SessionMessage message) {
        return "[%s/%s] %s".formatted(
                message.role().name().toLowerCase(),
                message.type().name().toLowerCase(),
                message.content()
        );
    }

    private String formatToolResult(ToolCallResult toolResult) {
        return "[%s] status=%s, message=%s, rawData=%s".formatted(
                toolResult.toolName(),
                toolResult.status(),
                toolResult.message(),
                toolResult.rawData()
        );
    }

    private String emptyToDash(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}
