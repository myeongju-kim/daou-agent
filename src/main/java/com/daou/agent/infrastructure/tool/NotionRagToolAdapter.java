package com.daou.agent.infrastructure.tool;

import com.daou.agent.domain.tool.ToolCallRequest;
import com.daou.agent.domain.tool.ToolCallResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotionRagToolAdapter implements ToolAdapter {

    private static final Pattern SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[\\s,./|:;!?()\\[\\]{}]+");
    private static final Set<String> KOREAN_PARTICLES = Set.of("을", "를", "이", "가", "은", "는", "와", "과", "도", "로");
    private static final Set<String> STOPWORDS = Set.of(
            "관련", "이슈", "요약", "정리", "모두", "전체", "찾아", "찾기", "조회", "해줘", "해주세요", "해달라", "부탁", "문서",
            "자료", "정보", "내용"
    );
    private static final Map<String, String> TYPO_REPLACEMENTS = Map.ofEntries(
            Map.entry("겔제", "결제"),
            Map.entry("결재", "결제"),
            Map.entry("로긴", "로그인"),
            Map.entry("로그인하기", "로그인")
    );

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
        String rawQuery = firstNonBlank(
                stringArgument(request, "query", ""),
                stringArgument(request, "keyword", ""),
                stringArgument(request, "q", "")
        );
        String correctedQuery = normalizeTypos(rawQuery);
        String pathQuery = stringArgument(request, "pathQuery", "");
        int limit = clamp(intArgument(request, "limit", defaultLimit), 1, maxLimit);

        try {
            Class.forName(driverClassName);
            try (Connection connection = openConnection()) {
                long totalDocuments = queryCount(connection, "SELECT count(*) FROM " + schema + ".documents");
                long totalChunks = queryCount(connection, "SELECT count(*) FROM " + schema + ".chunks");
                List<String> keywordCandidates = buildKeywordCandidates(rawQuery);
                List<Map<String, Object>> matches = List.of();
                String searchMode = "keyword";

                if (!keywordCandidates.isEmpty()) {
                    matches = queryChunksByKeywords(connection, keywordCandidates, pathQuery, limit);
                }
                if (matches.isEmpty() && !correctedQuery.isBlank()) {
                    matches = queryChunks(connection, correctedQuery, pathQuery, limit);
                    searchMode = keywordCandidates.isEmpty() ? "phrase" : "phrase_fallback";
                }

                Map<String, Object> raw = new LinkedHashMap<>();
                raw.put("schema", schema);
                raw.put("query", rawQuery);
                raw.put("correctedQuery", correctedQuery);
                raw.put("pathQuery", pathQuery);
                raw.put("limit", limit);
                raw.put("searchMode", searchMode);
                raw.put("keywordCandidates", keywordCandidates);
                raw.put("totalDocuments", totalDocuments);
                raw.put("totalChunks", totalChunks);
                raw.put("matchCount", matches.size());
                raw.put("matches", matches);

                String message = buildMessage(rawQuery, correctedQuery, pathQuery, totalDocuments, totalChunks, matches.size(), searchMode);
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

    private List<Map<String, Object>> queryChunksByKeywords(Connection connection, List<String> keywords, String pathQuery, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT d.path, c.chunk_index, c.content ")
                .append("FROM ").append(schema).append(".chunks c ")
                .append("JOIN ").append(schema).append(".documents d ON d.id = c.document_id");

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (!keywords.isEmpty()) {
            List<String> keywordConditions = new ArrayList<>();
            for (String keyword : keywords) {
                keywordConditions.add("c.content ILIKE ?");
                params.add("%" + keyword + "%");
            }
            conditions.add("(" + String.join(" OR ", keywordConditions) + ")");
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

    private String buildMessage(
            String rawQuery,
            String correctedQuery,
            String pathQuery,
            long totalDocuments,
            long totalChunks,
            int matchCount,
            String searchMode
    ) {
        if (!rawQuery.isBlank()) {
            if (!rawQuery.equals(correctedQuery)) {
                return "키워드 '%s'(보정: '%s') 검색 결과 %d건을 조회했습니다. (mode=%s, documents=%d, chunks=%d)"
                        .formatted(rawQuery, correctedQuery, matchCount, searchMode, totalDocuments, totalChunks);
            }
            return "키워드 '%s' 검색 결과 %d건을 조회했습니다. (documents=%d, chunks=%d)"
                    .formatted(rawQuery, matchCount, totalDocuments, totalChunks);
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

    private List<String> buildKeywordCandidates(String rawQuery) {
        String corrected = normalizeTypos(rawQuery);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String[] phraseTokens = TOKEN_SPLIT_PATTERN.split(corrected);
        if (phraseTokens.length == 1) {
            String normalizedSingle = stripKoreanParticle(corrected.trim());
            maybeAddCandidate(candidates, normalizedSingle);
        }

        for (String token : phraseTokens) {
            String normalizedToken = stripKoreanParticle(token.trim());
            normalizedToken = normalizeTypos(normalizedToken);
            maybeAddCandidate(candidates, normalizedToken);
            if (normalizedToken.endsWith("하기") && normalizedToken.length() > 2) {
                maybeAddCandidate(candidates, normalizedToken.substring(0, normalizedToken.length() - 2));
            }
        }

        if (candidates.size() > 8) {
            return new ArrayList<>(candidates).subList(0, 8);
        }
        return new ArrayList<>(candidates);
    }

    private void maybeAddCandidate(LinkedHashSet<String> candidates, String token) {
        if (token == null) {
            return;
        }
        String normalized = token.trim();
        if (normalized.length() < 2) {
            return;
        }
        if (STOPWORDS.contains(normalized)) {
            return;
        }
        candidates.add(normalized);
    }

    private String normalizeTypos(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.trim();
        for (Map.Entry<String, String> replacement : TYPO_REPLACEMENTS.entrySet()) {
            normalized = normalized.replace(replacement.getKey(), replacement.getValue());
        }
        return normalized;
    }

    private String stripKoreanParticle(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String normalized = token.trim();
        if (normalized.length() < 2) {
            return normalized;
        }
        String tail = normalized.substring(normalized.length() - 1);
        if (KOREAN_PARTICLES.contains(tail)) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
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
