package com.daou.agent.application.session;

public final class AgentSessionIdCodec {

    private static final String DELIMITER = "::";

    private AgentSessionIdCodec() {
    }

    public static String encode(String agentKey, String publicSessionId) {
        String normalizedAgent = normalizeOrThrow(agentKey, "agentKey");
        String normalizedSessionId = normalizeOrThrow(publicSessionId, "sessionId");
        return normalizedAgent + DELIMITER + normalizedSessionId;
    }

    public static String decodePublicId(String storageSessionId) {
        String normalized = normalizeOrThrow(storageSessionId, "storageSessionId");
        int delimiterIndex = normalized.indexOf(DELIMITER);
        if (delimiterIndex < 0) {
            return normalized;
        }
        return normalized.substring(delimiterIndex + DELIMITER.length());
    }

    public static String decodeAgentKey(String storageSessionId) {
        String normalized = normalizeOrThrow(storageSessionId, "storageSessionId");
        int delimiterIndex = normalized.indexOf(DELIMITER);
        if (delimiterIndex < 0) {
            return "";
        }
        return normalized.substring(0, delimiterIndex);
    }

    private static String normalizeOrThrow(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }
}
