package com.daou.agent.application.agent;

import com.daou.agent.application.session.MemoryService;
import com.daou.agent.application.session.SessionService;
import com.daou.agent.domain.agent.AgentContext;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.AgentStatus;
import com.daou.agent.domain.session.Session;
import org.springframework.stereotype.Service;

@Service
public class AgentRunner {

    private final SessionService sessionService;
    private final MemoryService memoryService;
    private final AgentLoopEngine agentLoopEngine;

    public AgentRunner(
            SessionService sessionService,
            MemoryService memoryService,
            AgentLoopEngine agentLoopEngine
    ) {
        this.sessionService = sessionService;
        this.memoryService = memoryService;
        this.agentLoopEngine = agentLoopEngine;
    }

    public AgentResult run(String sessionId, String message) {
        Session session = sessionService.getOrCreate(sessionId);

        sessionService.appendUserMessage(session.getId(), message);

        AgentContext context = memoryService.buildContext(session, message);
        AgentResult result = agentLoopEngine.execute(context);

        if (result.status() == AgentStatus.OK) {
            sessionService.appendAssistantMessage(session.getId(), result.message());
        }

        return result;
    }
}
