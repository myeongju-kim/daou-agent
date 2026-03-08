package com.daou.agent.support;

import com.daou.agent.application.agent.LlmResponse;
import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.tool.ToolCallRequest;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class TestLlmStubConfig {

    @Bean
    @Primary
    public LlmClient testLlmClient() {
        return this::decide;
    }

    private LlmResponse decide(AgentContext context) {
        String message = context.getCurrentUserMessage();

        if (!context.getToolResults().isEmpty()) {
            String resultSummary = context.getToolResults().get(context.getToolResults().size() - 1).message();
            return LlmResponse.finalAnswer("[stub] 도구 실행 결과입니다: " + resultSummary);
        }

        if (containsAny(message, "일정 등록", "회의 생성", "일정 추가")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "calendar.create_event",
                    Map.of("title", message)
            ));
        }

        if (containsAny(message, "일정", "캘린더")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "calendar.list_events",
                    Map.of("timeMin", "today", "timeMax", "next_7_days")
            ));
        }

        if (containsAny(message, "파일", "read")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "fs.read",
                    Map.of("path", "README.md")
            ));
        }

        if (containsAny(message, "http", "요청")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "http.request",
                    Map.of("method", "GET", "url", "https://portal.daou.co.kr")
            ));
        }

        return LlmResponse.finalAnswer("[stub] 요청을 이해했습니다: " + message);
    }

    private boolean containsAny(String source, String... tokens) {
        if (source == null) {
            return false;
        }
        for (String token : tokens) {
            if (source.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
