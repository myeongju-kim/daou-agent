package com.daou.agent.api.chat;

import com.daou.agent.api.common.ApiContract;
import com.daou.agent.application.agent.AgentRunner;
import com.daou.agent.domain.agent.AgentResult;
import com.daou.agent.domain.agent.LoopStep;
import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final AgentRunner agentRunner;

    public ChatController(AgentRunner agentRunner) {
        this.agentRunner = agentRunner;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        AgentResult result = agentRunner.run(request.sessionId(), request.message());
        List<String> stepDetails = result.steps().stream()
                .map(LoopStep::detail)
                .toList();

        return new ChatResponse(
                ApiContract.VERSION,
                result.status().name().toLowerCase(),
                result.message(),
                stepDetails,
                result.approvalId(),
                CorrelationIdHolder.getOrCreate()
        );
    }
}
