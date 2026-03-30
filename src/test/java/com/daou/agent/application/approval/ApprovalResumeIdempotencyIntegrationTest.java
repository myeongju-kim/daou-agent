package com.daou.agent.application.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.daou.agent.application.agent.AgentRunner;
import com.daou.agent.application.session.AgentSessionIdCodec;
import com.daou.agent.application.session.SessionService;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.support.TestLlmStubConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestLlmStubConfig.class)
class ApprovalResumeIdempotencyIntegrationTest {

    @Autowired
    private AgentRunner agentRunner;

    @Autowired
    private ApprovalResumeService approvalResumeService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TestLlmStubConfig.TestDaouPortalStubState stubState;

    @BeforeEach
    void setUp() {
        stubState.reset();
    }

    @Test
    void shouldCacheResumeResultForSameApprovalId() {
        AgentResult first = agentRunner.run("resume-idempotent", "내일 오전 10시에 일정 등록해줘");

        AgentResult resumed1 = approvalResumeService.approveAndResume(first.approvalId());
        AgentResult resumed2 = approvalResumeService.approveAndResume(first.approvalId());

        assertThat(resumed1.status()).isEqualTo(resumed2.status());
        assertThat(resumed1.message()).isEqualTo(resumed2.message());
        assertThat(stubState.calendarCreateCalls().get()).isEqualTo(1);
    }

    @Test
    void shouldRejectResumeWhenUserContextChanged() {
        AgentResult first = agentRunner.run("resume-context", "내일 오전 10시에 일정 등록해줘");
        sessionService.appendUserMessage(
                AgentSessionIdCodec.encode("personal", "resume-context"),
                "방금 요청 취소해줘"
        );

        assertThatThrownBy(() -> approvalResumeService.approveAndResume(first.approvalId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("사용자 메시지가 변경");
    }
}
