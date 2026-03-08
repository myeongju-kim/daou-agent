package com.daou.agent.application.agent;

import com.daou.agent.application.llm.OllamaModelService;
import com.daou.agent.application.session.MemoryService;
import com.daou.agent.application.session.SessionService;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.AgentStatus;
import com.daou.agent.domain.session.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgentRunner {

    private final SessionService sessionService;
    private final MemoryService memoryService;
    private final AgentLoopEngine agentLoopEngine;
    private final OllamaModelService ollamaModelService;
    private final String llmProvider;

    public AgentRunner(
            SessionService sessionService,
            MemoryService memoryService,
            AgentLoopEngine agentLoopEngine,
            OllamaModelService ollamaModelService,
            @Value("${agent.llm-provider:ollama}") String llmProvider
    ) {
        this.sessionService = sessionService;
        this.memoryService = memoryService;
        this.agentLoopEngine = agentLoopEngine;
        this.ollamaModelService = ollamaModelService;
        this.llmProvider = llmProvider;
    }

    public AgentResult run(String sessionId, String message) {
        Session session = sessionService.getOrCreate(sessionId);

        if ("ollama".equalsIgnoreCase(llmProvider) && session.getSelectedModel().isBlank()) {
            try {
                ollamaModelService.ensureSelectedModel(session.getId());
            } catch (Exception ignored) {
                // fallback 지정 실패 시 기존 default 모델 흐름으로 진행
            }
        }

        sessionService.appendUserMessage(session.getId(), message);

        AgentContext context = memoryService.buildContext(session, message);
        AgentResult result = agentLoopEngine.execute(context);

        if (result.status() == AgentStatus.OK) {
            sessionService.appendAssistantMessage(session.getId(), result.message());
        }

        return result;
    }
}
