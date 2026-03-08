package com.daou.agent.application.port;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;

public interface ToolExecutor {
    ToolCallResult execute(ToolCallRequest request);
}
