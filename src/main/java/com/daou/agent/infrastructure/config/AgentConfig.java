package com.daou.agent.infrastructure.config;

import com.daou.agent.application.port.LlmClient;
import com.daou.agent.domain.common.RiskLevel;
import com.daou.agent.domain.tool.ToolDefinition;
import com.daou.agent.domain.tool.ToolRegistry;
import com.daou.agent.infrastructure.external.daou.DaouPortalProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.daou.agent.infrastructure.llm.LlmClientFactory;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DaouPortalProperties.class)
public class AgentConfig {

    @Bean
    public ToolRegistry toolRegistry() {
        return new ToolRegistry(List.of(
                new ToolDefinition("calendar.list_calendars", "사용 가능한 캘린더 목록을 조회한다. 인자 없음.", RiskLevel.LOW),
                new ToolDefinition("calendar.list_events", "일정 목록을 조회한다. 인자: calendarIds(optional), timeMin(yyyy-MM-dd HH:mm), timeMax(yyyy-MM-dd HH:mm).", RiskLevel.LOW),
                new ToolDefinition("calendar.create_event", "일정을 생성한다. 인자: summary, startTime, endTime, calendarId(optional), location(optional).", RiskLevel.MEDIUM),
                new ToolDefinition("mail.list_folders", "메일함 목록을 조회한다. 인자 없음.", RiskLevel.LOW),
                new ToolDefinition("mail.list_messages", "메일 목록을 조회한다. 인자: folderId(optional), page(optional), size(optional), keyword(optional).", RiskLevel.LOW),
                new ToolDefinition("mail.read_message", "메일 상세를 조회한다. 인자: folder(optional, default Inbox), uid(required).", RiskLevel.LOW),
                new ToolDefinition("mail.send_message", "메일을 발송한다. 인자: to, subject, content.", RiskLevel.MEDIUM),
                new ToolDefinition("messenger.send_message", "1:1 메신저를 전송한다. 인자: toUser, message.", RiskLevel.MEDIUM),
                new ToolDefinition("http.request", "임의의 HTTP 요청을 실행한다. 인자: method, url, headers(optional), body(optional).", RiskLevel.MEDIUM),
                new ToolDefinition("fs.read", "로컬 파일을 조회한다. 현재 정책상 차단된다.", RiskLevel.HIGH)
        ));
    }

    @Bean
    public LlmClient llmClient(
            LlmClientFactory llmClientFactory,
            @Value("${agent.llm-provider:ollama}") String provider
    ) {
        return llmClientFactory.create(provider);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
