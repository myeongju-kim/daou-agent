package com.daou.agent.api.chat;

import java.util.List;

public record ChatResponse(
        String status,
        String message,
        List<String> steps,
        String approvalId
) {
}
