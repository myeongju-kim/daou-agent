package com.daou.agent.api.approval;

import com.daou.agent.domain.approval.ApprovalRequest;
import java.util.LinkedHashMap;
import java.util.Map;

public record ApprovalInfoResponse(
        String reason,
        String action,
        String toolName,
        String riskLevel,
        String requestedUserMessage,
        String preview,
        Map<String, Object> arguments
) {

    public static ApprovalInfoResponse from(ApprovalRequest request) {
        String action = actionLabel(request.getToolCall().toolName());
        Map<String, Object> arguments = new LinkedHashMap<>(request.getToolCall().arguments());
        return new ApprovalInfoResponse(
                action + "으로 인한 승인 요청입니다.",
                action,
                request.getToolCall().toolName(),
                request.getRiskLevel().name().toLowerCase(),
                request.getRequestedUserMessage(),
                preview(request.getToolCall().toolName(), arguments),
                arguments
        );
    }

    private static String actionLabel(String toolName) {
        return switch (toolName) {
            case "mail.send_message" -> "메일 발송";
            case "calendar.create_event" -> "일정 생성";
            case "messenger.send_message" -> "메신저 전송";
            default -> toolName + " 실행";
        };
    }

    private static String preview(String toolName, Map<String, Object> arguments) {
        if ("mail.send_message".equals(toolName)) {
            return "수신자: %s / 제목: %s / 내용: %s".formatted(
                    argument(arguments, "to"),
                    argument(arguments, "subject"),
                    argument(arguments, "content")
            );
        }
        if ("calendar.create_event".equals(toolName)) {
            return "제목: %s / 시작: %s / 종료: %s / 장소: %s".formatted(
                    argument(arguments, "summary"),
                    argument(arguments, "startTime"),
                    argument(arguments, "endTime"),
                    argument(arguments, "location")
            );
        }
        if ("messenger.send_message".equals(toolName)) {
            return "수신자: %s / 내용: %s".formatted(
                    argument(arguments, "toUser"),
                    argument(arguments, "message")
            );
        }
        return arguments.toString();
    }

    private static String argument(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            return "-";
        }
        String text = value.toString().trim();
        return text.isBlank() ? "-" : text;
    }
}

