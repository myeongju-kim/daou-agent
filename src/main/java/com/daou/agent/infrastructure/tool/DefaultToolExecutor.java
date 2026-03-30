package com.daou.agent.infrastructure.tool;

import com.daou.agent.application.port.ToolExecutor;
import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultToolExecutor implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultToolExecutor.class);

    private final List<ToolAdapter> adapters;
    private final ToolExecutionPolicyResolver policyResolver;

    public DefaultToolExecutor(List<ToolAdapter> adapters, ToolExecutionPolicyResolver policyResolver) {
        this.adapters = adapters;
        this.policyResolver = policyResolver;
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        ToolAdapter adapter = adapters.stream()
                .filter(candidate -> candidate.supports(request.toolName()))
                .findFirst()
                .orElse(null);

        if (adapter == null) {
            return ToolCallResult.failure(
                    request.toolName(),
                    "error",
                    "지원하지 않는 도구입니다.",
                    Map.of(),
                    false
            );
        }

        ToolExecutionPolicy policy = policyResolver.resolve(request.toolName());
        log.info("event=tool.execute.start toolName={} timeoutMs={} maxAttempts={} arguments={}",
                request.toolName(), policy.timeoutMillis(), policy.maxAttempts(), sanitizeArguments(request.arguments()));

        ToolCallResult lastResult = null;

        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            try {
                ToolCallResult result = executeOnce(adapter, request, policy);
                log.info("event=tool.execute.finish toolName={} attempt={} status={}",
                        request.toolName(), attempt, result.status());

                if (!result.retriable() || attempt == policy.maxAttempts()) {
                    return result;
                }
                lastResult = result;
            } catch (ToolExecutionException e) {
                log.warn("event=tool.execute.failed toolName={} attempt={} retriable={} message={}",
                        request.toolName(), attempt, e.retriable(), e.getMessage());
                if (!e.retriable() || attempt == policy.maxAttempts()) {
                    return ToolCallResult.failure(
                            request.toolName(),
                            "error",
                            e.getMessage(),
                            Map.of("attempt", attempt),
                            false
                    );
                }
                lastResult = ToolCallResult.failure(
                        request.toolName(),
                        "error",
                        e.getMessage(),
                        Map.of("attempt", attempt),
                        true
                );
            }
        }

        return lastResult == null
                ? ToolCallResult.failure(request.toolName(), "error", "도구 실행에 실패했습니다.", Map.of(), false)
                : ToolCallResult.failure(
                        request.toolName(),
                        lastResult.status(),
                        lastResult.message(),
                        lastResult.rawData(),
                        false
                );
    }

    private ToolCallResult executeOnce(ToolAdapter adapter, ToolCallRequest request, ToolExecutionPolicy policy) {
        String parentCorrelationId = CorrelationIdHolder.get();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<ToolCallResult> future = executor.submit(() -> {
                String previousCorrelationId = CorrelationIdHolder.get();
                try {
                    if (!parentCorrelationId.isBlank()) {
                        CorrelationIdHolder.set(parentCorrelationId);
                    }
                    return adapter.execute(request);
                } finally {
                    if (previousCorrelationId.isBlank()) {
                        CorrelationIdHolder.clear();
                    } else {
                        CorrelationIdHolder.set(previousCorrelationId);
                    }
                }
            });
            return future.get(policy.timeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new ToolExecutionException(
                    "도구 실행 시간이 초과되었습니다(timeout=%sms)".formatted(policy.timeoutMillis()),
                    true
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ToolExecutionException("도구 실행이 중단되었습니다.", true);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ToolExecutionException toolExecutionException) {
                throw toolExecutionException;
            }
            throw new ToolExecutionException(cause == null ? "도구 실행 실패" : cause.getMessage(), cause, true);
        }
    }

    private Map<String, Object> sanitizeArguments(Map<String, Object> arguments) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String key = entry.getKey();
            String lower = key.toLowerCase();
            if (lower.contains("token") || lower.contains("secret") || lower.contains("password")) {
                sanitized.put(key, "***");
                continue;
            }
            String text = String.valueOf(value);
            sanitized.put(key, text.length() <= 200 ? text : text.substring(0, 200) + "...(truncated)");
        }
        return sanitized;
    }
}
