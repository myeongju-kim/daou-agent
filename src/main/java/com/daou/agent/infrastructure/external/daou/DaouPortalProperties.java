package com.daou.agent.infrastructure.external.daou;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agent.daou-portal")
public class DaouPortalProperties {

    private String baseUrl = "https://portal.daou.co.kr/openapi2/v2";
    private String authUrl = "https://portal.daou.co.kr/oauth/token";
    private String clientId = "";
    private String clientSecret = "";
    private String username = "";
    private String password = "";
    private long defaultCalendarId = 7627L;
    private long defaultAttendeeId = 5397L;
    private long tokenRefreshSkewSeconds = 60L;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getDefaultCalendarId() {
        return defaultCalendarId;
    }

    public void setDefaultCalendarId(long defaultCalendarId) {
        this.defaultCalendarId = defaultCalendarId;
    }

    public long getDefaultAttendeeId() {
        return defaultAttendeeId;
    }

    public void setDefaultAttendeeId(long defaultAttendeeId) {
        this.defaultAttendeeId = defaultAttendeeId;
    }

    public long getTokenRefreshSkewSeconds() {
        return tokenRefreshSkewSeconds;
    }

    public void setTokenRefreshSkewSeconds(long tokenRefreshSkewSeconds) {
        this.tokenRefreshSkewSeconds = tokenRefreshSkewSeconds;
    }
}
