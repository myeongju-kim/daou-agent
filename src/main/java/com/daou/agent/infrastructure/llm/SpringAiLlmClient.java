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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class SpringAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmClient.class);

    private final String provider;
    private final ToolRegistry toolRegistry;
    private final LlmJsonResponseParser responseParser;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final RestClient ollamaRestClient;
    private final String defaultOllamaModel;
    private final int ollamaMaxRetries;
    private final int ollamaReadTimeoutMs;

    public SpringAiLlmClient(
            String provider,
            ChatModel chatModel,
            ToolRegistry toolRegistry,
            LlmJsonResponseParser responseParser,
            ObjectMapper objectMapper,
            String ollamaBaseUrl,
            String defaultOllamaModel,
            int ollamaConnectTimeoutMs,
            int ollamaReadTimeoutMs,
            int ollamaMaxRetries
    ) {
        this.provider = provider;
        this.toolRegistry = toolRegistry;
        this.responseParser = responseParser;
        this.objectMapper = objectMapper;
        this.chatClient = ChatClient.create(chatModel);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(ollamaConnectTimeoutMs);
        requestFactory.setReadTimeout(ollamaReadTimeoutMs);
        this.ollamaRestClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(ollamaBaseUrl)
                .build();
        this.defaultOllamaModel = defaultOllamaModel == null ? "" : defaultOllamaModel.trim();
        this.ollamaReadTimeoutMs = ollamaReadTimeoutMs;
        this.ollamaMaxRetries = Math.max(1, ollamaMaxRetries);
    }

    @Override
    public LlmResponse generate(AgentContext context) {
        String systemPrompt = buildSystemPrompt(context);
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

        Exception lastError = null;
        for (int attempt = 1; attempt <= ollamaMaxRetries; attempt++) {
            try {
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
            } catch (Exception e) {
                lastError = e;
                boolean hasNext = attempt < ollamaMaxRetries;
                log.warn(
                        "event=ollama.chat.retry attempt={}/{} timeoutMs={} reason={}",
                        attempt,
                        ollamaMaxRetries,
                        ollamaReadTimeoutMs,
                        e.getMessage()
                );
                if (!hasNext) {
                    break;
                }
                sleepBackoff(attempt);
            }
        }

        throw new IllegalStateException(
                "Ollama 호출 실패(retries=%d, readTimeoutMs=%d)".formatted(ollamaMaxRetries, ollamaReadTimeoutMs),
                lastError
        );
    }

    private void sleepBackoff(int attempt) {
        try {
            long delay = Math.min(2000L, 300L * attempt);
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildSystemPrompt(AgentContext context) {
        List<String> allowedTools = context.getAllowedToolNames().isEmpty()
                ? toolRegistry.names().stream().sorted(Comparator.naturalOrder()).toList()
                : context.getAllowedToolNames().stream().sorted(Comparator.naturalOrder()).toList();
        String tools = allowedTools.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
        String toolDescriptions = allowedTools.stream()
                .map(name -> toolRegistry.find(name)
                        .map(definition -> "- %s: %s".formatted(definition.name(), definition.description()))
                        .orElse("- %s".formatted(name)))
                .collect(Collectors.joining("\n"));
        String agentHint = context.getAgentSystemHint().isBlank() ? "-" : context.getAgentSystemHint();
        String intent = context.getIntent().isBlank() ? "-" : context.getIntent();
        String agentKey = context.getAgentKey().isBlank() ? "-" : context.getAgentKey();

        return """
                당신은 DaouOffice Agent 백엔드의 의사결정 LLM이다.
                응답은 반드시 JSON object 한 개로만 반환한다.
                설명 문장, 코드블록, 마크다운은 금지한다.

                현재 에이전트:
                - agentKey: %s
                - intent: %s
                - profileHint: %s

                사용 가능한 도구:
                %s

                도구 설명:
                %s

                JSON 스키마:
                1) 최종 답변
                {"type":"final","message":"사용자에게 보여줄 최종 답변"}

                2) 도구 호출
                {"type":"tool_call","toolName":"도구명","arguments":{"key":"value"}}

                규칙:
                - toolName은 반드시 사용 가능한 도구 목록 중 하나여야 한다.
                - 선택된 에이전트의 허용 도구 목록 밖 toolName은 절대 호출하지 않는다.
                - 도구 호출 시 설명에 나온 필수 인자를 빠짐없이 채운다.
                - 이미 toolResults가 존재하면 기본적으로 final을 반환한다.
                - 알 수 없는 값은 임의 생성하지 말고 final로 설명한다.
                - 메일 발송/일정 등록/메신저 전송 같은 실행형 요청에서는 final을 반환하지 말고
                  반드시 tool_call을 먼저 반환한다.
                - 도구 실행 전에는 "보냈습니다/발송했습니다/등록했습니다/하겠습니다" 같은 약속/완료 표현을 금지한다.
                - 일정 조회/브리핑 요청이면 calendar.list_events를 바로 호출하지 말고
                  calendar.list_calendars 결과로 calendarIds를 확보한 뒤 calendar.list_events를 호출한다.
                - 지수/환율/주식 전망 요청이면 quant.predict_market을 먼저 호출한다.
                - quant.predict_market 호출 시 horizon은 day/week/month 중 하나로 정규화한다.
                """.formatted(agentKey, intent, agentHint, tools, toolDescriptions);
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
                agentKey: %s
                intent: %s
                summary: %s
                selectedModel: %s
                currentUserMessage: %s

                recentMessages:
                %s

                toolResults:
                %s
                """.formatted(
                context.getSessionId(),
                emptyToDash(context.getAgentKey()),
                emptyToDash(context.getIntent()),
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
