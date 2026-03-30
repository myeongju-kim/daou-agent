package com.daou.agent.infrastructure.tool;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import com.daou.agent.infrastructure.external.daou.DaouPortalClient;
import com.daou.agent.infrastructure.external.daou.DaouPortalProperties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DaouCalendarToolAdapter implements ToolAdapter {

    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DaouPortalClient daouPortalClient;
    private final DaouPortalProperties properties;

    public DaouCalendarToolAdapter(DaouPortalClient daouPortalClient, DaouPortalProperties properties) {
        this.daouPortalClient = daouPortalClient;
        this.properties = properties;
    }

    @Override
    public boolean supports(String toolName) {
        return "calendar.list_calendars".equals(toolName)
                || "calendar.list_events".equals(toolName)
                || "calendar.create_event".equals(toolName);
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        return switch (request.toolName()) {
            case "calendar.list_calendars" -> listCalendars(request);
            case "calendar.list_events" -> listEvents(request);
            case "calendar.create_event" -> createEvent(request);
            default -> throw new IllegalArgumentException("지원하지 않는 calendar tool 입니다: " + request.toolName());
        };
    }

    private ToolCallResult listCalendars(ToolCallRequest request) {
        List<Map<String, Object>> calendars;
        try {
            calendars = daouPortalClient.listCalendars();
        } catch (ToolExecutionException e) {
            if (!isNotFound(e)) {
                throw e;
            }
            calendars = List.of(defaultCalendarFallback());
            return ToolCallResult.success(
                    request.toolName(),
                    "캘린더 목록 API가 404를 반환해 기본 캘린더 1건을 사용했습니다.",
                    Map.of("calendars", calendars)
            );
        }

        if (calendars.isEmpty()) {
            calendars = List.of(defaultCalendarFallback());
            return ToolCallResult.success(
                    request.toolName(),
                    "캘린더 목록이 비어 있어 기본 캘린더 1건을 사용했습니다.",
                    Map.of("calendars", calendars)
            );
        }

        return ToolCallResult.success(
                request.toolName(),
                "캘린더 목록 %d건을 조회했습니다.".formatted(calendars.size()),
                Map.of("calendars", calendars)
        );
    }

    private ToolCallResult listEvents(ToolCallRequest request) {
        String calendarIds = resolveCalendarIds(request);
        String timeMin = resolveTime(request.arguments().get("timeMin"), defaultStart());
        String timeMax = resolveTime(request.arguments().get("timeMax"), defaultEnd());
        List<Map<String, Object>> events = daouPortalClient.listEvents(calendarIds, timeMin, timeMax);

        return ToolCallResult.success(
                request.toolName(),
                "일정 %d건을 조회했습니다.".formatted(events.size()),
                Map.of(
                        "calendarIds", calendarIds,
                        "timeMin", timeMin,
                        "timeMax", timeMax,
                        "events", events
                )
        );
    }

    private ToolCallResult createEvent(ToolCallRequest request) {
        String summary = stringArgument(request, "summary", stringArgument(request, "title", ""));
        String startTime = stringArgument(request, "startTime", "");
        String endTime = stringArgument(request, "endTime", "");

        if (summary.isBlank() || startTime.isBlank() || endTime.isBlank()) {
            throw new ToolExecutionException("calendar.create_event는 summary/startTime/endTime 인자가 필요합니다.", false);
        }

        long calendarId = longArgument(request, "calendarId", properties.getDefaultCalendarId());
        long attendeeId = longArgument(request, "attendeeId", properties.getDefaultAttendeeId());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", stringArgument(request, "type", "normal"));
        payload.put("calendarId", calendarId);
        payload.put("attendees", List.of(Map.of("id", attendeeId)));
        payload.put("timeType", stringArgument(request, "timeType", "timed"));
        payload.put("summary", summary);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        payload.put("visibility", stringArgument(request, "visibility", "public"));
        payload.put("timeZoneOffset", stringArgument(request, "timeZoneOffset", "+09:00"));
        payload.put("location", stringArgument(request, "location", ""));

        Map<String, Object> result = daouPortalClient.createEvent(calendarId, payload);
        return ToolCallResult.success(
                request.toolName(),
                "일정을 등록했습니다.",
                result
        );
    }

    private String resolveTime(Object raw, String defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        String value = raw.toString().trim();
        if (value.isBlank()) {
            return defaultValue;
        }
        return switch (value) {
            case "today" -> defaultStart();
            case "next_7_days" -> defaultEnd();
            default -> value;
        };
    }

    private String defaultStart() {
        return LocalDate.now().atStartOfDay().format(DATETIME_FORMAT);
    }

    private String defaultEnd() {
        return LocalDate.now().plusDays(7).atTime(23, 59).format(DATETIME_FORMAT);
    }

    private String stringArgument(ToolCallRequest request, String key, String defaultValue) {
        Object value = request.arguments().get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString().trim();
        return text.isBlank() ? defaultValue : text;
    }

    private long longArgument(ToolCallRequest request, String key, long defaultValue) {
        Object value = request.arguments().get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String resolveCalendarIds(ToolCallRequest request) {
        String requested = stringArgument(request, "calendarIds", "");
        if (!requested.isBlank()) {
            return requested;
        }

        try {
            List<Map<String, Object>> calendars = daouPortalClient.listCalendars();
            String fromList = calendars.stream()
                    .map(calendar -> calendar.get("calendarId"))
                    .filter(value -> value != null && !value.toString().isBlank())
                    .map(Object::toString)
                    .distinct()
                    .reduce((left, right) -> left + "," + right)
                    .orElse("");
            if (!fromList.isBlank()) {
                return fromList;
            }
        } catch (ToolExecutionException ignored) {
            // calendarIds 미지정 상황에서 보조 조회 실패는 기본 캘린더로 fallback 한다.
        }

        return String.valueOf(properties.getDefaultCalendarId());
    }

    private Map<String, Object> defaultCalendarFallback() {
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("calendarId", properties.getDefaultCalendarId());
        fallback.put("calendarName", "기본 캘린더");
        fallback.put("calendarType", "normal");
        fallback.put("defaultCalendar", true);
        fallback.put("fallback", true);
        return fallback;
    }

    private boolean isNotFound(ToolExecutionException e) {
        return e.getMessage() != null && e.getMessage().contains("status=404");
    }
}
