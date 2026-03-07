package com.daou.agent.infrastructure.llm;

import com.daou.agent.application.agent.LlmResponse;
import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.tool.ToolCallRequest;
import java.util.Map;

public class OpenAiLlmClient implements LlmClient {

    @Override
    public LlmResponse generate(AgentContext context) {
        return decide(context, "openai");
    }

    static LlmResponse decide(AgentContext context, String provider) {
        String message = context.getCurrentUserMessage();

        if (!context.getToolResults().isEmpty()) {
            String resultSummary = context.getToolResults().get(context.getToolResults().size() - 1).message();
            return LlmResponse.finalAnswer("[" + provider + "] 도구 실행 결과입니다: " + resultSummary);
        }

        if (message.contains("일정 등록") || message.contains("회의 생성") || message.contains("일정 추가")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "calendar.create_event",
                    Map.of("title", message)
            ));
        }

        if (message.contains("일정") || message.contains("캘린더")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "calendar.list_events",
                    Map.of("timeMin", "today", "timeMax", "next_7_days")
            ));
        }

        if (message.contains("파일") || message.contains("read")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "fs.read",
                    Map.of("path", "README.md")
            ));
        }

        if (message.contains("http") || message.contains("요청")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "http.request",
                    Map.of("method", "GET", "url", "https://portal.daou.co.kr")
            ));
        }

        return LlmResponse.finalAnswer("[" + provider + "] 요청을 이해했습니다: " + message);
    }
}
