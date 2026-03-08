package com.daou.agent.application.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.daou.agent.support.TestLlmStubConfig;
import com.daou.agent.domain.agent.AgentResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestLlmStubConfig.class)
class AgentRunnerIntegrationTest {

    @Autowired
    private AgentRunner agentRunner;

    @Test
    void shouldRequireApprovalForCalendarCreateEvent() {
        AgentResult result = agentRunner.run("default", "내일 오전 10시에 일정 등록해줘");
        assertThat(result.status().name().toLowerCase()).isEqualTo("approval_required");
        assertThat(result.approvalId()).isNotBlank();
    }

    @Test
    void shouldReturnOkForSimpleMessage() {
        AgentResult result = agentRunner.run("default", "안녕");
        assertThat(result.status().name().toLowerCase()).isEqualTo("ok");
    }
}
