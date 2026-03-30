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
        this.policies = Map.ofEntries(
                Map.entry("calendar.list_events", new ToolExecutionPolicy(5000, 2)),
                Map.entry("calendar.list_calendars", new ToolExecutionPolicy(4000, 2)),
                Map.entry("calendar.create_event", new ToolExecutionPolicy(7000, 1)),
                Map.entry("mail.list_folders", new ToolExecutionPolicy(5000, 2)),
                Map.entry("mail.list_messages", new ToolExecutionPolicy(5000, 2)),
                Map.entry("mail.read_message", new ToolExecutionPolicy(5000, 2)),
                Map.entry("mail.send_message", new ToolExecutionPolicy(7000, 1)),
                Map.entry("messenger.send_message", new ToolExecutionPolicy(5000, 1)),
                Map.entry("quant.predict_market", new ToolExecutionPolicy(7000, 2)),
                Map.entry("rag.search_documents", new ToolExecutionPolicy(7000, 2)),
                Map.entry("http.request", new ToolExecutionPolicy(4000, 1))
        );
    }

    public ToolExecutionPolicy resolve(String toolName) {
        return policies.getOrDefault(toolName, defaultPolicy);
    }
}
