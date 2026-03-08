package com.daou.agent.application.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daou.agent.application.session.SessionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

class OllamaModelServiceTest {

    @Test
    void shouldReturnMappedModelsWhenProviderIsOllama() {
        OllamaApi ollamaApi = mock(OllamaApi.class);
        SessionService sessionService = mock(SessionService.class);
        OllamaApi.Model.Details details = new OllamaApi.Model.Details(
                "qwen2",
                "gguf",
                "qwen2",
                List.of("qwen2"),
                "7B",
                "Q4_0"
        );
        OllamaApi.Model model = new OllamaApi.Model(
                "qwen2.5:7b",
                "qwen2.5:7b",
                Instant.parse("2026-03-08T10:12:00Z"),
                4_653_242_378L,
                "sha256:abc",
                details
        );
        when(ollamaApi.listModels()).thenReturn(new OllamaApi.ListModelResponse(List.of(model)));

        OllamaModelService service = new OllamaModelService(beanProvider(ollamaApi), "ollama", sessionService);
        OllamaModelSummary summary = service.getModels();

        assertThat(summary.provider()).isEqualTo("ollama");
        assertThat(summary.modelCount()).isEqualTo(1);
        assertThat(summary.models()).hasSize(1);
        assertThat(summary.models().get(0).name()).isEqualTo("qwen2.5:7b");
        assertThat(summary.models().get(0).parameterSize()).isEqualTo("7B");
    }

    @Test
    void shouldRejectWhenProviderIsNotOllama() {
        SessionService sessionService = mock(SessionService.class);
        OllamaModelService service = new OllamaModelService(beanProvider(null), "openai", sessionService);

        assertThatThrownBy(service::getModels)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ollama가 아닙니다");
    }

    @Test
    void shouldSaveSelectedModelWhenModelExists() {
        OllamaApi ollamaApi = mock(OllamaApi.class);
        SessionService sessionService = mock(SessionService.class);
        OllamaApi.Model model = new OllamaApi.Model(
                "qwen2.5:7b",
                "qwen2.5:7b",
                Instant.parse("2026-03-08T10:12:00Z"),
                4_653_242_378L,
                "sha256:abc",
                null
        );
        when(ollamaApi.listModels()).thenReturn(new OllamaApi.ListModelResponse(List.of(model)));

        OllamaModelService service = new OllamaModelService(beanProvider(ollamaApi), "ollama", sessionService);
        String selected = service.selectModel("default", "qwen2.5:7b");

        assertThat(selected).isEqualTo("qwen2.5:7b");
        verify(sessionService).setSelectedModel("default", "qwen2.5:7b");
    }

    private ObjectProvider<OllamaApi> beanProvider(OllamaApi ollamaApi) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        if (ollamaApi != null) {
            beanFactory.addBean("ollamaApi", ollamaApi);
        }
        return beanFactory.getBeanProvider(OllamaApi.class);
    }
}
