package com.daou.agent.api.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.daou.agent.support.TestLlmStubConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.agentKey").value("personal"))
                .andExpect(jsonPath("$.intent").isNotEmpty());
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
    void shouldExposeAgentProfilesForFrontendRouting() throws Exception {
        mockMvc.perform(get("/agents"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.version").value("v1"))
                .andExpect(jsonPath("$.defaultAgentKey").value("personal"))
                .andExpect(jsonPath("$.agents.length()").isNotEmpty())
                .andExpect(jsonPath("$.agents[?(@.key=='personal')]").isNotEmpty())
                .andExpect(jsonPath("$.agents[?(@.key=='quant-trainer')]").isNotEmpty());
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

    @Test
    void shouldExposeApprovalDetailsWhenApprovalIsRequired() throws Exception {
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "contract-approval",
                                  "message": "내일 오전 10시에 일정 등록해줘"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approval_required"))
                .andExpect(jsonPath("$.approvalId").isNotEmpty())
                .andExpect(jsonPath("$.agentKey").value("personal"))
                .andExpect(jsonPath("$.intent").value("office.calendar"))
                .andExpect(jsonPath("$.approval.reason").isNotEmpty())
                .andExpect(jsonPath("$.approval.action").value("일정 생성"))
                .andExpect(jsonPath("$.approval.toolName").value("calendar.create_event"))
                .andExpect(jsonPath("$.approval.riskLevel").value("medium"))
                .andExpect(jsonPath("$.approval.preview").isNotEmpty())
                .andExpect(jsonPath("$.approval.arguments.summary").isNotEmpty());
    }

    @Test
    void shouldExposeApprovalDetailsOnApproveResponse() throws Exception {
        MvcResult chatResult = mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "contract-approve",
                                  "message": "내일 오전 10시에 일정 등록해줘"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String approvalId = JsonPath.read(chatResult.getResponse().getContentAsString(), "$.approvalId");

        mockMvc.perform(post("/approvals/{id}/approve", approvalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalId").value(approvalId))
                .andExpect(jsonPath("$.approval.reason").isNotEmpty())
                .andExpect(jsonPath("$.approval.action").value("일정 생성"))
                .andExpect(jsonPath("$.approval.toolName").value("calendar.create_event"))
                .andExpect(jsonPath("$.approval.arguments.summary").isNotEmpty());
    }
}
