package com.daou.agent.application.agent;

import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class IntentRuleResolver {

    public String resolve(String agentKey, String message) {
        String normalizedAgent = normalize(agentKey);
        String normalizedMessage = normalize(message);

        if ("quant-trainer".equals(normalizedAgent)) {
            if (containsAny(normalizedMessage, "코스피", "환율", "원달러", "주가", "종목", "전망", "예측", "nvidia", "apple", "tesla", "microsoft")) {
                return "quant.forecast";
            }
            return "quant.general";
        }

        if (containsAny(normalizedMessage, "일정", "캘린더", "회의", "스케줄")) {
            return "office.calendar";
        }
        if (containsAny(normalizedMessage, "메일", "email", "이메일", "mail")) {
            return "office.mail";
        }
        if (containsAny(normalizedMessage, "메신저", "메시지", "쪽지", "chat", "dm")) {
            return "office.messenger";
        }
        return "office.general";
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).trim();
    }
}
