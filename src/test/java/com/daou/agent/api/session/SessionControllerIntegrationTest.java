package com.daou.agent.api.session;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daou.agent.application.agent.AgentRunner;
import com.daou.agent.support.TestLlmStubConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "agent.storage.type=jdbc",
        "spring.datasource.url=jdbc:h2:mem:sessionapi;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@Import(TestLlmStubConfig.class)
class SessionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentRunner agentRunner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM agent_approval_requests");
        jdbcTemplate.update("DELETE FROM agent_session_messages");
        jdbcTemplate.update("DELETE FROM agent_sessions");
        agentRunner.run("personal", "room-alpha", "안녕");
        agentRunner.run("personal", "room-bravo", "오늘 일정 알려줘");
        agentRunner.run("quant-trainer", "room-other", "엔비디아 내일 전망 알려줘");
    }

    @Test
    void shouldListSessionsByAgentKey() throws Exception {
        mockMvc.perform(get("/sessions")
                        .queryParam("agentKey", "personal"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.agentKey").value("personal"))
                .andExpect(jsonPath("$.sessions.length()").value(2))
                .andExpect(jsonPath("$.sessions[0].sessionId").value("room-bravo"))
                .andExpect(jsonPath("$.sessions[0].lastMessage").isNotEmpty())
                .andExpect(jsonPath("$.sessions[1].sessionId").value("room-alpha"));
    }

    @Test
    void shouldReturnSessionDetailWithMessages() throws Exception {
        mockMvc.perform(get("/sessions/{sessionId}", "room-bravo")
                        .queryParam("agentKey", "personal"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.sessionId").value("room-bravo"))
                .andExpect(jsonPath("$.agentKey").value("personal"))
                .andExpect(jsonPath("$.messages.length()").value(3))
                .andExpect(jsonPath("$.messages[0].role").value("user"))
                .andExpect(jsonPath("$.messages[1].role").value("tool"))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"));
    }

    @Test
    void shouldReturnNotFoundWhenSessionDoesNotExist() throws Exception {
        mockMvc.perform(get("/sessions/{sessionId}", "missing-room")
                        .queryParam("agentKey", "personal"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }
}
