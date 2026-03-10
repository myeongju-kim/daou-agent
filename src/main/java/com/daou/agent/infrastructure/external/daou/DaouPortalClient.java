package com.daou.agent.infrastructure.external.daou;

import com.daou.agent.infrastructure.tool.ToolExecutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        return asList(get("/v1/calendar/list", Map.of()));
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
            throw convertError(path, e);
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
        log.info("event=daou.api.call method={} path={} refreshed={}", method.name(), path, refreshed);

        RestClient.RequestBodySpec requestSpec = restClient.method(method)
                .uri(uriBuilder -> buildUri(uriBuilder, path, queryParams))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Object> response = body == null
                ? requestSpec.retrieve().toEntity(Object.class)
                : requestSpec.body(body).retrieve().toEntity(Object.class);

        return objectMapper.valueToTree(response.getBody() == null ? Map.of() : response.getBody());
    }

    private String buildUri(UriBuilder uriBuilder, String path, Map<String, ?> queryParams) {
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
        return current.build().toString();
    }

    private ToolExecutionException convertError(String path, RestClientResponseException e) {
        String message = "Daou Portal API 호출 실패(path=%s, status=%s)".formatted(path, e.getStatusCode().value());
        log.warn("event=daou.api.call.failed path={} status={} message={}", path, e.getStatusCode().value(), e.getMessage());
        return new ToolExecutionException(message, e, e.getStatusCode().is5xxServerError());
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
