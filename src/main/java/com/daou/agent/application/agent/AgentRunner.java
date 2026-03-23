package com.daou.agent.application.agent;

import com.daou.agent.application.llm.OllamaModelService;
import com.daou.agent.application.session.MemoryService;
import com.daou.agent.application.session.SessionService;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.AgentStatus;
import com.daou.agent.domain.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgentRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentRunner.class);

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
        return run(Session.DEFAULT_AGENT_KEY, sessionId, message);
    }

    public AgentResult run(String agentKey, String sessionId, String message) {
        log.info("event=agent.run.start sessionId={} messageLength={}", sessionId, message == null ? 0 : message.length());
        Session session = sessionService.getOrCreate(sessionId, agentKey);

        if ("ollama".equalsIgnoreCase(llmProvider) && session.getSelectedModel().isBlank()) {
            try {
                ollamaModelService.ensureSelectedModel(session.getId());
            } catch (Exception ignored) {
                // fallback 지정 실패 시 기존 default 모델 흐름으로 진행
            }
        }

        sessionService.appendUserMessage(session.getId(), message);
        session = sessionService.getOrCreate(sessionId, agentKey);

        AgentContext context = memoryService.buildContext(session, message);
        AgentResult result = agentLoopEngine.execute(context);

        if (result.status() == AgentStatus.OK) {
            sessionService.appendAssistantMessage(session.getId(), result.message());
        }

        log.info("event=agent.run.finish sessionId={} status={} approvalId={}",
                sessionId, result.status().name(), result.approvalId());
        return result;
    }
}
