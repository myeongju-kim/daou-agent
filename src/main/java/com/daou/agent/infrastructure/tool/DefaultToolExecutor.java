package com.daou.agent.infrastructure.tool;

import com.daou.agent.application.port.ToolExecutor;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DefaultToolExecutor implements ToolExecutor {

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        return switch (request.toolName()) {
            case "calendar.list_events" -> new ToolCallResult(
                    request.toolName(),
                    "ok",
                    "오늘 일정 2건을 조회했습니다.",
                    Map.of(
                            "date", LocalDate.now().toString(),
                            "events", new String[]{"09:30 Daily Standup", "14:00 Sprint Planning"}
                    ),
                    false
            );
            case "calendar.create_event" -> new ToolCallResult(
                    request.toolName(),
                    "ok",
                    "일정 생성 요청이 접수되었습니다.",
                    Map.of("title", request.arguments().getOrDefault("title", "새 일정")),
                    false
            );
            case "http.request" -> new ToolCallResult(
                    request.toolName(),
                    "ok",
                    "HTTP 요청 시뮬레이션이 완료되었습니다.",
                    Map.of("statusCode", 200),
                    true
            );
            case "fs.read" -> new ToolCallResult(
                    request.toolName(),
                    "ok",
                    "파일 읽기 시뮬레이션 결과입니다.",
                    Map.of("path", request.arguments().getOrDefault("path", "README.md")),
                    false
            );
            default -> new ToolCallResult(
                    request.toolName(),
                    "error",
                    "지원하지 않는 도구입니다.",
                    Map.of(),
                    false
            );
        };
    }
}
