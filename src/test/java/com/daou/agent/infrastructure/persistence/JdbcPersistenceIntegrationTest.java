package com.daou.agent.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.daou.agent.application.agent.AgentRunner;
import com.daou.agent.application.approval.ApprovalService;
import com.daou.agent.application.port.SessionRepository;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.approval.ApprovalRequest;
import com.daou.agent.domain.common.ApprovalStatus;
import com.daou.agent.domain.session.Session;
import com.daou.agent.support.TestLlmStubConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "agent.storage.type=jdbc",
        "spring.datasource.url=jdbc:h2:mem:daouagent;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@Import(TestLlmStubConfig.class)
class JdbcPersistenceIntegrationTest {

    @Autowired
    private AgentRunner agentRunner;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ApprovalService approvalService;

    @Test
    void shouldPersistDaouOfficeChatRoomMessages() {
        AgentResult result = agentRunner.run("daouoffice", "jdbc-session", "안녕");

        assertThat(result.status().name().toLowerCase()).isEqualTo("ok");

        Session session = sessionRepository.findById("jdbc-session").orElseThrow();
        assertThat(session.getAgentKey()).isEqualTo("daouoffice");
        assertThat(session.getTitle()).isEqualTo("안녕");
        assertThat(session.getMessages()).hasSize(2);
        assertThat(session.getMessages().get(0).content()).isEqualTo("안녕");
        assertThat(session.getMessages().get(1).content()).contains("[stub] 요청을 이해했습니다");
    }

    @Test
    void shouldPersistApprovalRequestInJdbcStore() {
        AgentResult result = agentRunner.run("daouoffice", "jdbc-approval", "내일 오전 10시에 일정 등록해줘");

        assertThat(result.status().name().toLowerCase()).isEqualTo("approval_required");
        ApprovalRequest approvalRequest = approvalService.getById(result.approvalId());

        assertThat(approvalRequest.getSessionId()).isEqualTo("jdbc-approval");
        assertThat(approvalRequest.getToolCall().toolName()).isEqualTo("calendar.create_event");
        assertThat(approvalRequest.getStatus()).isEqualTo(ApprovalStatus.PENDING);
    }
}
