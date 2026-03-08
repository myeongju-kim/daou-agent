package com.daou.agent.application.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.daou.agent.application.agent.AgentRunner;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.support.TestLlmStubConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestLlmStubConfig.class)
class ApprovalResumeServiceIntegrationTest {

    @Autowired
    private AgentRunner agentRunner;

    @Autowired
    private ApprovalResumeService approvalResumeService;

    @Test
    void shouldResumeAfterApprove() {
        AgentResult first = agentRunner.run("resume-session", "내일 오전 10시에 일정 등록해줘");
        assertThat(first.status().name().toLowerCase()).isEqualTo("approval_required");
        assertThat(first.approvalId()).isNotBlank();

        AgentResult resumed = approvalResumeService.approveAndResume(first.approvalId());
        assertThat(resumed.status().name().toLowerCase()).isEqualTo("ok");
        assertThat(resumed.steps().stream().map(step -> step.type()).toList())
                .contains("approval_approved", "tool_result", "think");
    }
}
