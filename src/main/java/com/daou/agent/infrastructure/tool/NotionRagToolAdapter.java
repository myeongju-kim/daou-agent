package com.daou.agent.infrastructure.tool;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotionRagToolAdapter implements ToolAdapter {

    private static final Pattern SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    private final String schema;
    private final int defaultLimit;
    private final int maxLimit;
    private final int previewLength;

    public NotionRagToolAdapter(
            @Value("${agent.rag.datasource.url:jdbc:postgresql://localhost:5432/postgres}") String jdbcUrl,
            @Value("${agent.rag.datasource.username:${user.name}}") String username,
            @Value("${agent.rag.datasource.password:}") String password,
            @Value("${agent.rag.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName,
            @Value("${agent.rag.schema:notion_rag}") String schema,
            @Value("${agent.rag.default-limit:10}") int defaultLimit,
            @Value("${agent.rag.max-limit:20}") int maxLimit,
            @Value("${agent.rag.preview-length:120}") int previewLength
    ) {
        this.jdbcUrl = normalize(jdbcUrl);
        this.username = normalize(username);
        this.password = password == null ? "" : password;
        this.driverClassName = normalize(driverClassName);
        this.schema = normalize(schema);
        this.defaultLimit = clamp(defaultLimit, 1, 20);
        this.maxLimit = clamp(maxLimit, 1, 100);
        this.previewLength = clamp(previewLength, 40, 400);

        if (this.jdbcUrl.isBlank()) {
            throw new IllegalArgumentException("agent.rag.datasource.url 설정이 필요합니다.");
        }
        if (this.driverClassName.isBlank()) {
            throw new IllegalArgumentException("agent.rag.datasource.driver-class-name 설정이 필요합니다.");
        }
        if (!SCHEMA_PATTERN.matcher(this.schema).matches()) {
            throw new IllegalArgumentException("agent.rag.schema 값이 유효하지 않습니다: " + this.schema);
        }
    }

    @Override
    public boolean supports(String toolName) {
        return "rag.search_documents".equals(toolName);
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        String query = firstNonBlank(
                stringArgument(request, "query", ""),
                stringArgument(request, "keyword", ""),
                stringArgument(request, "q", "")
        );
        String pathQuery = stringArgument(request, "pathQuery", "");
        int limit = clamp(intArgument(request, "limit", defaultLimit), 1, maxLimit);

        try {
            Class.forName(driverClassName);
            try (Connection connection = openConnection()) {
                long totalDocuments = queryCount(connection, "SELECT count(*) FROM " + schema + ".documents");
                long totalChunks = queryCount(connection, "SELECT count(*) FROM " + schema + ".chunks");
                List<Map<String, Object>> matches = queryChunks(connection, query, pathQuery, limit);

                Map<String, Object> raw = new LinkedHashMap<>();
                raw.put("schema", schema);
                raw.put("query", query);
                raw.put("pathQuery", pathQuery);
                raw.put("limit", limit);
                raw.put("totalDocuments", totalDocuments);
                raw.put("totalChunks", totalChunks);
                raw.put("matchCount", matches.size());
                raw.put("matches", matches);

                String message = buildMessage(query, pathQuery, totalDocuments, totalChunks, matches.size());
                return ToolCallResult.success(request.toolName(), message, raw);
            }
        } catch (ClassNotFoundException e) {
            throw new ToolExecutionException("RAG DB 드라이버를 찾을 수 없습니다: " + driverClassName, false);
        } catch (SQLException e) {
            throw new ToolExecutionException("RAG DB 조회 실패: " + rootMessage(e), true);
        }
    }

    private Connection openConnection() throws SQLException {
        if (username.isBlank()) {
            return DriverManager.getConnection(jdbcUrl);
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private long queryCount(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        }
    }

    private List<Map<String, Object>> queryChunks(Connection connection, String query, String pathQuery, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT d.path, c.chunk_index, c.content ")
                .append("FROM ").append(schema).append(".chunks c ")
                .append("JOIN ").append(schema).append(".documents d ON d.id = c.document_id");

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (!query.isBlank()) {
            conditions.add("c.content ILIKE ?");
            params.add("%" + query + "%");
        }
        if (!pathQuery.isBlank()) {
            conditions.add("d.path ILIKE ?");
            params.add("%" + pathQuery + "%");
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        sql.append(" ORDER BY d.path, c.chunk_index LIMIT ?");
        params.add(limit);

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("path", rs.getString("path"));
                    row.put("chunkIndex", rs.getInt("chunk_index"));
                    row.put("preview", abbreviate(rs.getString("content"), previewLength));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private String buildMessage(String query, String pathQuery, long totalDocuments, long totalChunks, int matchCount) {
        if (!query.isBlank()) {
            return "키워드 '%s' 검색 결과 %d건을 조회했습니다. (documents=%d, chunks=%d)"
                    .formatted(query, matchCount, totalDocuments, totalChunks);
        }
        if (!pathQuery.isBlank()) {
            return "경로 키워드 '%s' 기준으로 %d건을 조회했습니다. (documents=%d, chunks=%d)"
                    .formatted(pathQuery, matchCount, totalDocuments, totalChunks);
        }
        return "문서 DB 상태를 조회했습니다. (documents=%d, chunks=%d, sample=%d)"
                .formatted(totalDocuments, totalChunks, matchCount);
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        String message = throwable.getMessage();
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
            if (cursor.getMessage() != null && !cursor.getMessage().isBlank()) {
                message = cursor.getMessage();
            }
        }
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
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
        String text = value.toString().trim();
        if (text.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
