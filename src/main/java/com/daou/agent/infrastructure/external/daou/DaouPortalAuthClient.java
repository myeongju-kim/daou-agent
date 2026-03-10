package com.daou.agent.infrastructure.external.daou;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class DaouPortalAuthClient {

    private static final Logger log = LoggerFactory.getLogger(DaouPortalAuthClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final DaouPortalProperties properties;
    private volatile AccessToken cachedToken;

    public DaouPortalAuthClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            DaouPortalProperties properties
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.getAuthUrl()).build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String getAccessToken() {
        AccessToken accessToken = cachedToken;
        if (isUsable(accessToken)) {
            return accessToken.value();
        }

        synchronized (this) {
            accessToken = cachedToken;
            if (isUsable(accessToken)) {
                return accessToken.value();
            }

            AccessToken refreshed = requestToken();
            cachedToken = refreshed;
            return refreshed.value();
        }
    }

    public void evict() {
        cachedToken = null;
    }

    private boolean isUsable(AccessToken accessToken) {
        if (accessToken == null) {
            return false;
        }
        Instant threshold = Instant.now().plusSeconds(properties.getTokenRefreshSkewSeconds());
        return accessToken.expiresAt().isAfter(threshold);
    }

    private AccessToken requestToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("username", properties.getUsername());
        formData.add("password", properties.getPassword());

        Map<?, ?> response = restClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);

        JsonNode root = objectMapper.valueToTree(response == null ? Map.of() : response);
        String accessToken = root.path("access_token").asText("");
        if (accessToken.isBlank()) {
            throw new IllegalStateException("Daou Portal access token 응답이 비어 있습니다.");
        }

        Instant expiresAt = decodeJwtExpiry(accessToken);
        log.info("event=daou.auth.token.refreshed expiresAt={}", expiresAt);
        return new AccessToken(accessToken, expiresAt);
    }

    private Instant decodeJwtExpiry(String accessToken) {
        try {
            String[] chunks = accessToken.split("\\.");
            if (chunks.length < 2) {
                return Instant.now().plusSeconds(3600);
            }
            String payload = new String(Base64.getUrlDecoder().decode(chunks[1]), StandardCharsets.UTF_8);
            JsonNode node = objectMapper.readTree(payload);
            long exp = node.path("exp").asLong(0L);
            if (exp <= 0L) {
                return Instant.now().plusSeconds(3600);
            }
            return Instant.ofEpochSecond(exp);
        } catch (Exception e) {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("message", e.getMessage());
            log.warn("event=daou.auth.token.expiry.decode_failed context={}", context);
            return Instant.now().plusSeconds(3600);
        }
    }

    private record AccessToken(String value, Instant expiresAt) {
    }
}
