package com.daou.agent.infrastructure.llm;

import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class LlmClientFactory {

    private final ObjectProvider<OpenAiChatModel> openAiChatModelProvider;
    private final ObjectProvider<OllamaChatModel> ollamaChatModelProvider;
    private final LlmJsonResponseParser responseParser;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final String ollamaBaseUrl;
    private final String defaultOllamaModel;
    private final int ollamaConnectTimeoutMs;
    private final int ollamaReadTimeoutMs;
    private final int ollamaMaxRetries;

    public LlmClientFactory(
            ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
            ObjectProvider<OllamaChatModel> ollamaChatModelProvider,
            LlmJsonResponseParser responseParser,
            ToolRegistry toolRegistry,
            ObjectMapper objectMapper,
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${spring.ai.ollama.chat.options.model:}") String defaultOllamaModel,
            @Value("${agent.ollama.connect-timeout-ms:5000}") int ollamaConnectTimeoutMs,
            @Value("${agent.ollama.read-timeout-ms:120000}") int ollamaReadTimeoutMs,
            @Value("${agent.ollama.max-retries:2}") int ollamaMaxRetries
    ) {
        this.openAiChatModelProvider = openAiChatModelProvider;
        this.ollamaChatModelProvider = ollamaChatModelProvider;
        this.responseParser = responseParser;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.defaultOllamaModel = defaultOllamaModel;
        this.ollamaConnectTimeoutMs = ollamaConnectTimeoutMs;
        this.ollamaReadTimeoutMs = ollamaReadTimeoutMs;
        this.ollamaMaxRetries = ollamaMaxRetries;
    }

    public LlmClient create(String provider) {
        if (provider == null) {
            return createWithProvider("ollama");
        }
        return createWithProvider(provider.toLowerCase());
    }

    private LlmClient createWithProvider(String provider) {
        return switch (provider) {
            case "openai" -> new SpringAiLlmClient(
                    provider,
                    requireModel(openAiChatModelProvider, "openAiChatModel"),
                    toolRegistry,
                    responseParser,
                    objectMapper,
                    ollamaBaseUrl,
                    defaultOllamaModel,
                    ollamaConnectTimeoutMs,
                    ollamaReadTimeoutMs,
                    ollamaMaxRetries
            );
            case "ollama" -> new SpringAiLlmClient(
                    provider,
                    requireModel(ollamaChatModelProvider, "ollamaChatModel"),
                    toolRegistry,
                    responseParser,
                    objectMapper,
                    ollamaBaseUrl,
                    defaultOllamaModel,
                    ollamaConnectTimeoutMs,
                    ollamaReadTimeoutMs,
                    ollamaMaxRetries
            );
            default -> throw new IllegalArgumentException("unsupported llm provider: " + provider);
        };
    }

    private ChatModel requireModel(ObjectProvider<? extends ChatModel> provider, String beanName) {
        ChatModel chatModel = provider.getIfAvailable();
        if (chatModel == null) {
            throw new IllegalStateException("LLM model bean is not available: " + beanName);
        }
        return chatModel;
    }
}
