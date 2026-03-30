package com.daou.agent.support;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.daou.agent.application.agent.LlmResponse;
import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.infrastructure.external.daou.DaouPortalClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Bean
    public TestDaouPortalStubState testDaouPortalStubState() {
        return new TestDaouPortalStubState();
    }

    @Bean
    @Primary
    public DaouPortalClient testDaouPortalClient(TestDaouPortalStubState state) {
        DaouPortalClient client = mock(DaouPortalClient.class);
        when(client.listCalendars()).thenReturn(List.of(
                Map.of("calendarId", 7627, "calendarName", "내 일정", "defaultCalendar", true)
        ));
        when(client.listEvents(anyString(), anyString(), anyString())).thenAnswer(invocation -> List.of(
                Map.of(
                        "calendarId", invocation.getArgument(0),
                        "summary", "테스트 일정",
                        "startTime", invocation.getArgument(1),
                        "endTime", invocation.getArgument(2)
                )
        ));
        when(client.createEvent(anyLong(), anyMap())).thenAnswer(invocation -> {
            state.calendarCreateCalls().incrementAndGet();
            Map<?, ?> payload = invocation.getArgument(1);
            return Map.of(
                    "id", "event-1",
                    "calendarId", invocation.getArgument(0),
                    "summary", payload.get("summary"),
                    "startTime", payload.get("startTime"),
                    "endTime", payload.get("endTime")
            );
        });
        when(client.listMailFolders()).thenReturn(Map.of(
                "defaultFolders", List.of(Map.of("id", "defaultFolder0", "name", "받은메일함"))
        ));
        when(client.listMailMessages(anyString(), anyInt(), anyInt(), anyString())).thenReturn(Map.of(
                "messageCount", 1,
                "messageList", List.of(Map.of("id", 1001, "subject", "테스트 메일"))
        ));
        when(client.readMailMessage(anyString(), anyString())).thenReturn(Map.of(
                "msgContent", Map.of("subject", "테스트 메일", "uid", 1001)
        ));
        when(client.sendMail(anyString(), anyString(), anyString())).thenReturn(Map.of(
                "saveSent", true,
                "totalAddressCount", 1
        ));
        when(client.sendMessengerMessage(anyString(), anyString())).thenReturn(Map.of(
                "resultCode", 0,
                "message", "SUCCESS"
        ));
        return client;
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
                    Map.of(
                            "summary", message,
                            "startTime", "2026-03-11 10:00",
                            "endTime", "2026-03-11 11:00",
                            "location", "회의실A"
                    )
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

        if (containsAny(message, "코스피", "환율", "삼성전자", "엔비디아", "테슬라", "애플", "주가", "전망")) {
            return LlmResponse.toolCall(new ToolCallRequest(
                    "quant.predict_market",
                    Map.of(
                            "target", message,
                            "horizon", "day"
                    )
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

    public static class TestDaouPortalStubState {
        private final AtomicInteger calendarCreateCalls = new AtomicInteger();

        public AtomicInteger calendarCreateCalls() {
            return calendarCreateCalls;
        }

        public void reset() {
            calendarCreateCalls.set(0);
        }
    }
}
