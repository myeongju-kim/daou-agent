package com.daou.agent.infrastructure.tool;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import com.daou.agent.infrastructure.external.daou.DaouPortalClient;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DaouMessengerToolAdapter implements ToolAdapter {

    private final DaouPortalClient daouPortalClient;

    public DaouMessengerToolAdapter(DaouPortalClient daouPortalClient) {
        this.daouPortalClient = daouPortalClient;
    }

    @Override
    public boolean supports(String toolName) {
        return "messenger.send_message".equals(toolName);
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String toUser = stringArgument(request, "toUser", "");
        String message = stringArgument(request, "message", "");
        if (toUser.isBlank() || message.isBlank()) {
            throw new ToolExecutionException("messenger.send_message는 toUser/message 인자가 필요합니다.", false);
        }
        Map<String, Object> result = daouPortalClient.sendMessengerMessage(toUser, message);
        return ToolCallResult.success(request.toolName(), "메신저 전송 요청을 처리했습니다.", result);
    }

    private String stringArgument(ToolCallRequest request, String key, String defaultValue) {
        Object value = request.arguments().get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString().trim();
        return text.isBlank() ? defaultValue : text;
    }
}
