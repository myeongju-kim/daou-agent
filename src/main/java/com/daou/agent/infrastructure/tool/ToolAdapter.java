package com.daou.agent.infrastructure.tool;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;

public interface ToolAdapter {

    boolean supports(String toolName);

    ToolCallResult execute(ToolCallRequest request);
}
