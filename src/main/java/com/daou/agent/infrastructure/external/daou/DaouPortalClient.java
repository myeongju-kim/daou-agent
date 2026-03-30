package com.daou.agent.infrastructure.external.daou;

import com.daou.agent.infrastructure.tool.ToolExecutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

@Component
public class DaouPortalClient {

    private static final Logger log = LoggerFactory.getLogger(DaouPortalClient.class);
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization",
            "token",
            "password",
            "secret",
            "content",
            "message",
            "subject",
            "body"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DaouPortalAuthClient authClient;

    public DaouPortalClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            DaouPortalAuthClient authClient,
            DaouPortalProperties properties
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
        this.objectMapper = objectMapper;
        this.authClient = authClient;
    }

    public List<Map<String, Object>> listCalendars() {
        JsonNode response = getWithFallback(List.of("/v1/calendar/list", "/v1/calendar"), Map.of());
        return asList(response);
    }

    public List<Map<String, Object>> listEvents(String calendarIds, String timeMin, String timeMax) {
        return asList(get("/v1/calendar/event", Map.of(
                "calendarIds", calendarIds,
                "timeMin", timeMin,
                "timeMax", timeMax
        )));
    }

    public Map<String, Object> createEvent(long calendarId, Map<String, Object> body) {
        return asMap(post("/v1/calendar/%s/event".formatted(calendarId), body));
    }

    public Map<String, Object> listMailFolders() {
        return asMap(get("/v1/mail/folder/all", Map.of()));
    }

    public Map<String, Object> listMailMessages(String folderId, int page, int size, String keyword) {
        return asMap(get("/v1/mail/message/list", Map.of(
                "folderId", folderId,
                "page", page,
                "size", size,
                "keyword", keyword
        )));
    }

    public Map<String, Object> readMailMessage(String folder, String uid) {
        return asMap(get("/v1/mail/message/read", Map.of(
                "folder", folder,
                "uid", uid
        )));
    }

    public Map<String, Object> sendMail(String to, String subject, String content) {
        return asMap(postWithQuery("/v1/mail/message/send", Map.of(
                "to", to,
                "subject", subject,
                "content", content
        )));
    }

    public Map<String, Object> sendMessengerMessage(String toUser, String message) {
        return asMap(post("/v1/chat/message/single", Map.of(
                "toUser", toUser,
                "message", message
        )));
    }

    private JsonNode get(String path, Map<String, ?> queryParams) {
        return exchange(HttpMethod.GET, path, queryParams, null);
    }

    private JsonNode getWithFallback(List<String> paths, Map<String, ?> queryParams) {
        ToolExecutionException lastError = null;
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            try {
                return get(path, queryParams);
            } catch (ToolExecutionException e) {
                lastError = e;
                boolean hasNext = i < paths.size() - 1;
                if (!hasNext || !isNotFoundError(e)) {
                    throw e;
                }
                String nextPath = paths.get(i + 1);
                log.warn(
                        "event=daou.api.call.fallback fromPath={} toPath={} reason={}",
                        path,
                        nextPath,
                        e.getMessage()
                );
            }
        }

        throw lastError == null
                ? new ToolExecutionException("Daou Portal API 호출 경로를 찾지 못했습니다.", false)
                : lastError;
    }

    private JsonNode post(String path, Object body) {
        return exchange(HttpMethod.POST, path, Map.of(), body);
    }

    private JsonNode postWithQuery(String path, Map<String, ?> queryParams) {
        return exchange(HttpMethod.POST, path, queryParams, null);
    }

    private JsonNode exchange(HttpMethod method, String path, Map<String, ?> queryParams, Object body) {
        try {
            return exchangeOnce(method, path, queryParams, body, false);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                authClient.evict();
                return exchangeOnce(method, path, queryParams, body, true);
            }
            throw convertError(path, queryParams, body, e);
        }
    }

    private JsonNode exchangeOnce(
            HttpMethod method,
            String path,
            Map<String, ?> queryParams,
            Object body,
            boolean refreshed
    ) {
        String token = authClient.getAccessToken();
        Map<String, Object> sanitizedQuery = sanitizeParams(queryParams);
        String bodySummary = summarizeBody(body);
        String requestUri = buildRequestUri(path, sanitizedQuery);
        log.info(
                "event=daou.api.call method={} path={} query={} uri={} body={} refreshed={}",
                method.name(),
                path,
                sanitizedQuery,
                requestUri,
                bodySummary,
                refreshed
        );

        RestClient.RequestBodySpec requestSpec = restClient.method(method)
                .uri(uriBuilder -> buildUri(uriBuilder, path, queryParams))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Object> response = body == null
                ? requestSpec.retrieve().toEntity(Object.class)
                : requestSpec.body(body).retrieve().toEntity(Object.class);

        return objectMapper.valueToTree(response.getBody() == null ? Map.of() : response.getBody());
    }

    private URI buildUri(UriBuilder uriBuilder, String path, Map<String, ?> queryParams) {
        UriBuilder current = uriBuilder.path(path);
        for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String text = value.toString();
            if (text.isBlank()) {
                continue;
            }
            current = current.queryParam(entry.getKey(), text);
        }
        return current.build();
    }

    private ToolExecutionException convertError(
            String path,
            Map<String, ?> queryParams,
            Object body,
            RestClientResponseException e
    ) {
        Map<String, Object> sanitizedQuery = sanitizeParams(queryParams);
        String bodySummary = summarizeBody(body);
        String message = "Daou Portal API 호출 실패(path=%s, status=%s)".formatted(path, e.getStatusCode().value());
        log.warn(
                "event=daou.api.call.failed path={} query={} body={} status={} message={}",
                path,
                sanitizedQuery,
                bodySummary,
                e.getStatusCode().value(),
                e.getMessage()
        );
        return new ToolExecutionException(message, e, e.getStatusCode().is5xxServerError());
    }

    private boolean isNotFoundError(ToolExecutionException e) {
        return e.getMessage() != null && e.getMessage().contains("status=404");
    }

    private Map<String, Object> sanitizeParams(Map<String, ?> params) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String text = value.toString().trim();
            if (text.isBlank()) {
                continue;
            }
            if (isSensitiveKey(entry.getKey())) {
                sanitized.put(entry.getKey(), "***");
                continue;
            }
            sanitized.put(entry.getKey(), trimForLog(text));
        }
        return sanitized;
    }

    private String summarizeBody(Object body) {
        if (body == null) {
            return "-";
        }
        if (body instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (isSensitiveKey(key)) {
                    converted.put(key, "***");
                } else {
                    converted.put(key, trimForLog(String.valueOf(value)));
                }
            }
            return converted.toString();
        }
        return trimForLog(body.toString());
    }

    private String buildRequestUri(String path, Map<String, Object> sanitizedQuery) {
        if (sanitizedQuery.isEmpty()) {
            return path;
        }
        StringBuilder builder = new StringBuilder(path).append('?');
        int index = 0;
        for (Map.Entry<String, Object> entry : sanitizedQuery.entrySet()) {
            if (index++ > 0) {
                builder.append('&');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private boolean isSensitiveKey(String key) {
        String lower = key == null ? "" : key.toLowerCase();
        for (String sensitive : SENSITIVE_KEYS) {
            if (lower.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    private String trimForLog(String value) {
        if (value.length() <= 200) {
            return value;
        }
        return value.substring(0, 200) + "...(truncated)";
    }

    private Map<String, Object> asMap(JsonNode node) {
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
    }

    private List<Map<String, Object>> asList(JsonNode node) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!node.isArray()) {
            return result;
        }
        for (JsonNode child : node) {
            result.add(asMap(child));
        }
        return result;
    }
}
