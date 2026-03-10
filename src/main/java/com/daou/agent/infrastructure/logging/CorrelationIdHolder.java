package com.daou.agent.infrastructure.logging;

import java.util.UUID;
import org.slf4j.MDC;

public final class CorrelationIdHolder {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIdHolder() {
    }

    public static String get() {
        String correlationId = MDC.get(MDC_KEY);
        return correlationId == null ? "" : correlationId;
    }

    public static String getOrCreate() {
        String correlationId = get();
        if (!correlationId.isBlank()) {
            return correlationId;
        }
        String generated = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, generated);
        return generated;
    }

    public static void set(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            MDC.remove(MDC_KEY);
            return;
        }
        MDC.put(MDC_KEY, correlationId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }
}
