package com.daou.agent.domain.tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ToolRegistry {
    private final Map<String, ToolDefinition> definitions;

    public ToolRegistry(List<ToolDefinition> definitions) {
        this.definitions = definitions.stream()
                .collect(Collectors.toUnmodifiableMap(ToolDefinition::name, def -> def));
    }

    public Optional<ToolDefinition> find(String name) {
        return Optional.ofNullable(definitions.get(name));
    }

    public Set<String> names() {
        return definitions.keySet();
    }
}
