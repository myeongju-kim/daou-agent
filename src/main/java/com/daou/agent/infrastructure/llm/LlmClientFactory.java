package com.daou.agent.infrastructure.llm;

import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.tool.ToolRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class LlmClientFactory {

    private final ObjectProvider<OpenAiChatModel> openAiChatModelProvider;
    private final ObjectProvider<OllamaChatModel> ollamaChatModelProvider;
    private final LlmJsonResponseParser responseParser;
    private final ToolRegistry toolRegistry;

    public LlmClientFactory(
            ObjectProvider<OpenAiChatModel> openAiChatModelProvider,
            ObjectProvider<OllamaChatModel> ollamaChatModelProvider,
            LlmJsonResponseParser responseParser,
            ToolRegistry toolRegistry
    ) {
        this.openAiChatModelProvider = openAiChatModelProvider;
        this.ollamaChatModelProvider = ollamaChatModelProvider;
        this.responseParser = responseParser;
        this.toolRegistry = toolRegistry;
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
                    responseParser
            );
            case "ollama" -> new SpringAiLlmClient(
                    provider,
                    requireModel(ollamaChatModelProvider, "ollamaChatModel"),
                    toolRegistry,
                    responseParser
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
