package com.daou.agent.infrastructure.tool;

public class ToolExecutionException extends RuntimeException {

    private final boolean retriable;

    public ToolExecutionException(String message, boolean retriable) {
        super(message);
        this.retriable = retriable;
    }

    public ToolExecutionException(String message, Throwable cause, boolean retriable) {
        super(message, cause);
        this.retriable = retriable;
    }

    public boolean retriable() {
        return retriable;
    }
}
