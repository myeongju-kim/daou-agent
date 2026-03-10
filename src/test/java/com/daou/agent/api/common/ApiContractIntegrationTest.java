package com.daou.agent.api.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.daou.agent.support.TestLlmStubConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestLlmStubConfig.class)
class ApiContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeVersionAndCorrelationIdOnChatResponse() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "contract-chat",
                                  "message": "안녕"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void shouldExposeVersionAndCorrelationIdOnDashboardResponse() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void shouldExposeErrorCodeOnValidationFailure() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "",
                                  "message": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
}
