package com.daou.agent.infrastructure.tool;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GenericHttpToolAdapter implements ToolAdapter {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GenericHttpToolAdapter(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String toolName) {
        return "http.request".equals(toolName);
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String method = request.arguments().getOrDefault("method", "GET").toString();
        String url = request.arguments().getOrDefault("url", "").toString();
        if (url.isBlank()) {
            throw new ToolExecutionException("http.request는 url 인자가 필요합니다.", false);
        }

        Map<String, Object> headers = mapArgument(request.arguments().get("headers"));
        Object body = request.arguments().get("body");

        try {
            RestClient.RequestBodySpec requestSpec = restClient.method(HttpMethod.valueOf(method.toUpperCase()))
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(httpHeaders -> headers.forEach((key, value) -> httpHeaders.add(key, value.toString())));

            ResponseEntity<Object> response = body == null
                    ? requestSpec.retrieve().toEntity(Object.class)
                    : requestSpec.body(body).retrieve().toEntity(Object.class);

            Map<String, Object> rawData = new LinkedHashMap<>();
            rawData.put("url", url);
            rawData.put("method", method.toUpperCase());
            rawData.put("statusCode", response.getStatusCode().value());
            rawData.put("response", response.getBody() == null ? Map.of() : response.getBody());

            return ToolCallResult.success(request.toolName(), "HTTP 요청을 완료했습니다.", rawData);
        } catch (RestClientResponseException e) {
            throw new ToolExecutionException(
                    "HTTP 요청 실패(status=%s)".formatted(e.getStatusCode().value()),
                    e,
                    e.getStatusCode().is5xxServerError()
            );
        }
    }

    private Map<String, Object> mapArgument(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        return objectMapper.convertValue(raw, objectMapper.getTypeFactory()
                .constructMapType(LinkedHashMap.class, String.class, Object.class));
    }
}
