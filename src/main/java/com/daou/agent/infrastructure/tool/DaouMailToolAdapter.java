package com.daou.agent.infrastructure.tool;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import com.daou.agent.infrastructure.external.daou.DaouPortalClient;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DaouMailToolAdapter implements ToolAdapter {

    private final DaouPortalClient daouPortalClient;

    public DaouMailToolAdapter(DaouPortalClient daouPortalClient) {
        this.daouPortalClient = daouPortalClient;
    }

    @Override
    public boolean supports(String toolName) {
        return "mail.list_folders".equals(toolName)
                || "mail.list_messages".equals(toolName)
                || "mail.read_message".equals(toolName)
                || "mail.send_message".equals(toolName);
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        return switch (request.toolName()) {
            case "mail.list_folders" -> ToolCallResult.success(
                    request.toolName(),
                    "메일함 목록을 조회했습니다.",
                    daouPortalClient.listMailFolders()
            );
            case "mail.list_messages" -> listMessages(request);
            case "mail.read_message" -> readMessage(request);
            case "mail.send_message" -> sendMessage(request);
            default -> throw new IllegalArgumentException("지원하지 않는 mail tool 입니다: " + request.toolName());
        };
    }

    private ToolCallResult listMessages(ToolCallRequest request) {
        String folderId = stringArgument(request, "folderId", "");
        int page = intArgument(request, "page", 1);
        int size = intArgument(request, "size", 20);
        String keyword = stringArgument(request, "keyword", "");
        Map<String, Object> result = daouPortalClient.listMailMessages(folderId, page, size, keyword);
        return ToolCallResult.success(request.toolName(), "메일 목록을 조회했습니다.", result);
    }

    private ToolCallResult readMessage(ToolCallRequest request) {
        String folder = stringArgument(request, "folder", "Inbox");
        String uid = stringArgument(request, "uid", "");
        if (uid.isBlank()) {
            throw new ToolExecutionException("mail.read_message는 uid 인자가 필요합니다.", false);
        }
        Map<String, Object> result = daouPortalClient.readMailMessage(folder, uid);
        return ToolCallResult.success(request.toolName(), "메일 상세를 조회했습니다.", result);
    }

    private ToolCallResult sendMessage(ToolCallRequest request) {
        String to = stringArgument(request, "to", "");
        String subject = stringArgument(request, "subject", "");
        String content = stringArgument(request, "content", "");
        if (to.isBlank() || subject.isBlank() || content.isBlank()) {
            throw new ToolExecutionException("mail.send_message는 to/subject/content 인자가 필요합니다.", false);
        }
        Map<String, Object> result = daouPortalClient.sendMail(to, subject, content);
        return ToolCallResult.success(request.toolName(), "메일 발송 요청을 처리했습니다.", result);
    }

    private String stringArgument(ToolCallRequest request, String key, String defaultValue) {
        Object value = request.arguments().get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = value.toString().trim();
        return text.isBlank() ? defaultValue : text;
    }

    private int intArgument(ToolCallRequest request, String key, int defaultValue) {
        Object value = request.arguments().get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
