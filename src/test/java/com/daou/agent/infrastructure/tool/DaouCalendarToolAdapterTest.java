package com.daou.agent.infrastructure.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import com.daou.agent.infrastructure.external.daou.DaouPortalClient;
import com.daou.agent.infrastructure.external.daou.DaouPortalProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DaouCalendarToolAdapterTest {

    @Test
    void shouldReturnCalendarListWhenPortalCallSucceeds() {
        DaouPortalClient client = mock(DaouPortalClient.class);
        DaouPortalProperties properties = defaultProperties();
        when(client.listCalendars()).thenReturn(List.of(
                Map.of("calendarId", 7627L, "calendarName", "내 일정", "defaultCalendar", true)
        ));

        DaouCalendarToolAdapter adapter = new DaouCalendarToolAdapter(client, properties);
        ToolCallResult result = adapter.execute(new ToolCallRequest("calendar.list_calendars", Map.of()));

        assertThat(result.status()).isEqualTo("ok");
        assertThat(result.message()).isEqualTo("캘린더 목록 1건을 조회했습니다.");
        assertThat(result.rawData().get("calendars")).asList().hasSize(1);
    }

    @Test
    void shouldFallbackToDefaultCalendarWhenListEndpointReturns404() {
        DaouPortalClient client = mock(DaouPortalClient.class);
        DaouPortalProperties properties = defaultProperties();
        properties.setDefaultCalendarId(9999L);
        when(client.listCalendars())
                .thenThrow(new ToolExecutionException("Daou Portal API 호출 실패(path=/v1/calendar/list, status=404)", false));

        DaouCalendarToolAdapter adapter = new DaouCalendarToolAdapter(client, properties);
        ToolCallResult result = adapter.execute(new ToolCallRequest("calendar.list_calendars", Map.of()));

        assertThat(result.status()).isEqualTo("ok");
        assertThat(result.message()).contains("기본 캘린더 1건");
        Object rawCalendars = result.rawData().get("calendars");
        assertThat(rawCalendars).asList().hasSize(1);
        Map<?, ?> fallback = (Map<?, ?>) ((List<?>) rawCalendars).get(0);
        assertThat(fallback.get("calendarId")).isEqualTo(9999L);
        assertThat(fallback.get("fallback")).isEqualTo(true);
    }

    @Test
    void shouldThrowWhenListEndpointFailsWithNon404() {
        DaouPortalClient client = mock(DaouPortalClient.class);
        DaouPortalProperties properties = defaultProperties();
        when(client.listCalendars())
                .thenThrow(new ToolExecutionException("Daou Portal API 호출 실패(path=/v1/calendar/list, status=500)", true));

        DaouCalendarToolAdapter adapter = new DaouCalendarToolAdapter(client, properties);

        assertThatThrownBy(() -> adapter.execute(new ToolCallRequest("calendar.list_calendars", Map.of())))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("status=500");
    }

    @Test
    void shouldResolveCalendarIdsFromListWhenListEventsHasNoCalendarIdsArgument() {
        DaouPortalClient client = mock(DaouPortalClient.class);
        DaouPortalProperties properties = defaultProperties();
        when(client.listCalendars()).thenReturn(List.of(
                Map.of("calendarId", 1001L),
                Map.of("calendarId", 1002L)
        ));
        when(client.listEvents(eq("1001,1002"), eq("2026-03-31 00:00"), eq("2026-03-31 23:59")))
                .thenReturn(List.of(Map.of("id", "evt-1")));

        DaouCalendarToolAdapter adapter = new DaouCalendarToolAdapter(client, properties);
        ToolCallResult result = adapter.execute(new ToolCallRequest(
                "calendar.list_events",
                Map.of("timeMin", "2026-03-31 00:00", "timeMax", "2026-03-31 23:59")
        ));

        assertThat(result.status()).isEqualTo("ok");
        assertThat(result.rawData().get("calendarIds")).isEqualTo("1001,1002");
        verify(client).listEvents("1001,1002", "2026-03-31 00:00", "2026-03-31 23:59");
    }

    private DaouPortalProperties defaultProperties() {
        DaouPortalProperties properties = new DaouPortalProperties();
        properties.setDefaultCalendarId(7627L);
        properties.setDefaultAttendeeId(5397L);
        return properties;
    }
}
