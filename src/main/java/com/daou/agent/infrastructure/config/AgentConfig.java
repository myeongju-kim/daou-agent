package com.daou.agent.infrastructure.config;

import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolDefinition;
import com.daou.agent.domain.tool.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.daou.agent.infrastructure.llm.LlmClientFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentConfig {

    @Bean
    public ToolRegistry toolRegistry() {
        return new ToolRegistry(List.of(
                new ToolDefinition("calendar.list_events", "일정 목록 조회", RiskLevel.LOW),
                new ToolDefinition("calendar.create_event", "일정 생성", RiskLevel.MEDIUM),
                new ToolDefinition("http.request", "HTTP 요청 실행", RiskLevel.MEDIUM),
                new ToolDefinition("fs.read", "로컬 파일 조회", RiskLevel.HIGH)
        ));
    }

    @Bean
    public LlmClient llmClient(
            LlmClientFactory llmClientFactory,
            @Value("${agent.llm-provider:ollama}") String provider
    ) {
        return llmClientFactory.create(provider);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
