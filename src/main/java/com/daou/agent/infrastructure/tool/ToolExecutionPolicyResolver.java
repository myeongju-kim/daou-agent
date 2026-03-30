package com.daou.agent.infrastructure.tool;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutionPolicyResolver {

    private final ToolExecutionPolicy defaultPolicy;
    private final Map<String, ToolExecutionPolicy> policies;

    public ToolExecutionPolicyResolver(
            @Value("${agent.tools.default-timeout-ms:5000}") int defaultTimeoutMillis,
            @Value("${agent.tools.default-max-attempts:2}") int defaultMaxAttempts
    ) {
        this.defaultPolicy = new ToolExecutionPolicy(defaultTimeoutMillis, defaultMaxAttempts);
        this.policies = Map.of(
                "calendar.list_events", new ToolExecutionPolicy(5000, 2),
                "calendar.list_calendars", new ToolExecutionPolicy(4000, 2),
                "calendar.create_event", new ToolExecutionPolicy(7000, 1),
                "mail.list_folders", new ToolExecutionPolicy(5000, 2),
                "mail.list_messages", new ToolExecutionPolicy(5000, 2),
                "mail.read_message", new ToolExecutionPolicy(5000, 2),
                "mail.send_message", new ToolExecutionPolicy(7000, 1),
                "messenger.send_message", new ToolExecutionPolicy(5000, 1),
                "quant.predict_market", new ToolExecutionPolicy(7000, 2),
                "http.request", new ToolExecutionPolicy(4000, 1)
        );
    }

    public ToolExecutionPolicy resolve(String toolName) {
        return policies.getOrDefault(toolName, defaultPolicy);
    }
}
