package com.daou.agent.application.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgentProfileService {

    private final String defaultAgentKey;
    private final Map<String, AgentProfile> profiles;
    private final Map<String, String> aliases;

    public AgentProfileService(@Value("${agent.default-agent-key:personal}") String defaultAgentKey) {
        this.defaultAgentKey = normalize(defaultAgentKey);
        this.profiles = buildProfiles();
        this.aliases = Map.of(
                "daouoffice", "personal",
                "daou-office", "personal",
                "quant", "quant-trainer"
        );
        if (!profiles.containsKey(this.defaultAgentKey)) {
            throw new IllegalStateException("기본 agentKey를 찾을 수 없습니다: " + this.defaultAgentKey);
        }
    }

    public String defaultAgentKey() {
        return defaultAgentKey;
    }

    public String normalizeAgentKey(String rawAgentKey) {
        String normalized = normalize(rawAgentKey);
        if (normalized.isBlank()) {
            return defaultAgentKey;
        }
        String aliased = aliases.getOrDefault(normalized, normalized);
        if (!profiles.containsKey(aliased)) {
            throw new IllegalArgumentException("지원하지 않는 agentKey 입니다: " + rawAgentKey);
        }
        return aliased;
    }

    public AgentProfile getProfile(String rawAgentKey) {
        String key = normalizeAgentKey(rawAgentKey);
        AgentProfile profile = profiles.get(key);
        if (profile == null) {
            throw new NoSuchElementException("agent profile not found: " + key);
        }
        return profile;
    }

    public List<AgentProfile> listProfiles() {
        return List.copyOf(profiles.values());
    }

    private Map<String, AgentProfile> buildProfiles() {
        Map<String, AgentProfile> built = new LinkedHashMap<>();

        built.put("personal", new AgentProfile(
                "personal",
                "Personal Assistant",
                "메일/일정/메신저 중심의 개인 업무 에이전트",
                "당신은 개인 업무 자동화 에이전트다. 일정/메일/메신저 요청을 우선 처리한다.",
                Set.of(
                        "calendar.list_calendars",
                        "calendar.list_events",
                        "calendar.create_event",
                        "mail.list_folders",
                        "mail.list_messages",
                        "mail.read_message",
                        "mail.send_message",
                        "messenger.send_message"
                ),
                List.of("office.calendar", "office.mail", "office.messenger", "office.general")
        ));

        built.put("quant-trainer", new AgentProfile(
                "quant-trainer",
                "Quant Trainer",
                "지수/환율/종목 예측 리포트 생성 에이전트",
                "당신은 시장 데이터 기반 전망 리포트를 만드는 퀀트 트레이너다. 예측 근거와 리스크를 함께 제시한다.",
                Set.of("quant.predict_market"),
                List.of("quant.forecast", "quant.general")
        ));

        return Map.copyOf(built);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
