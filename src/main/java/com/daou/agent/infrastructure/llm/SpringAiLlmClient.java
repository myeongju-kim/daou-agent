package com.daou.agent.infrastructure.llm;

import com.daou.agent.application.agent.LlmResponse;
import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.session.SessionMessage;
import com.daou.agent.domain.tool.ToolCallResult;
import com.daou.agent.domain.tool.ToolRegistry;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaChatOptions;

public class SpringAiLlmClient implements LlmClient {

    private final String provider;
    private final ToolRegistry toolRegistry;
    private final LlmJsonResponseParser responseParser;
    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public SpringAiLlmClient(
            String provider,
            ChatModel chatModel,
            ToolRegistry toolRegistry,
            LlmJsonResponseParser responseParser
    ) {
        this.provider = provider;
        this.toolRegistry = toolRegistry;
        this.responseParser = responseParser;
        this.chatModel = chatModel;
        this.chatClient = ChatClient.create(chatModel);
    }

    @Override
    public LlmResponse generate(AgentContext context) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(context);

        try {
            String selectedModel = context.getSelectedModel();

            // Ollama는 Prompt + OllamaChatOptions 경로로 모델 override를 강제 적용한다.
            if ("ollama".equalsIgnoreCase(provider) && !selectedModel.isBlank()) {
                String raw = callWithOllamaModelOverride(systemPrompt, userPrompt, selectedModel);
                return responseParser.parse(raw);
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

    private String callWithOllamaModelOverride(String systemPrompt, String userPrompt, String selectedModel) {
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                OllamaChatOptions.builder().model(selectedModel).build()
        );
        return chatModel.call(prompt).getResult().getOutput().getText();
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
