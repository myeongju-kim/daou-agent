package com.daou.agent.infrastructure.tool;

public record ToolExecutionPolicy(int timeoutMillis, int maxAttempts) {

    public ToolExecutionPolicy {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
    }
}
