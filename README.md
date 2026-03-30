# personal-agent-backend

개인용 멀티 에이전트 Spring AI Backend MVP입니다.

## 기술 스택
- Java 21
- Spring Boot 4.0.3
- Spring AI 2.0.0-M2
- Gradle

## 시작하기
### 요구사항
- JDK 21
- Gradle Wrapper 사용 가능 환경

### 실행
```bash
./gradlew bootRun
```

### 테스트
```bash
./gradlew test
```

### 빌드
```bash
./gradlew build
```

## 아키텍처 (MVP)
`domain/application/infrastructure/api` 경계 기반 구조로 구성되어 있습니다.

핵심 구성:
- Session / Memory
- AgentRunner / AgentLoop
- ToolRegistry / ToolExecutor
- ApprovalPolicy / Approval API
- LLM Provider Factory (`openai`, `ollama`)

## API
- `POST /chat`
- `GET /agents`
- `GET /dashboard`
- `GET /ollama/models`
- `POST /ollama/models/select`
- `GET /ollama/models/select/{sessionId}`
- `POST /approvals/{id}/approve`
- `POST /approvals/{id}/reject`
- `POST /approvals/{id}/approve-and-resume`

응답 계약은 phase3부터 `version: "v1"` 와 `correlationId`를 고정 제공합니다.

### Chat 요청 예시
```text
POST /chat
{
  "sessionId": "default",
  "message": "내일 오전 10시에 일정 등록해줘"
}
```

### Chat 응답 예시
```text
{
  "version": "v1",
  "status": "approval_required",
  "message": "승인이 필요한 도구입니다: calendar.create_event",
  "steps": ["loop=1", "calendar.create_event"],
  "approvalId": "...",
  "agentKey": "personal",
  "intent": "office.calendar",
  "correlationId": "..."
}
```

### Agent 목록 조회 예시
```text
GET /agents
{
  "version": "v1",
  "defaultAgentKey": "personal",
  "agents": [
    {
      "key": "personal",
      "name": "Personal Assistant",
      "description": "메일/일정/메신저 중심의 개인 업무 에이전트",
      "isDefault": true,
      "supportedIntents": ["office.calendar", "office.mail", "office.messenger", "office.general"],
      "allowedTools": ["calendar.list_events", "..."]
    },
    {
      "key": "quant-trainer",
      "name": "Quant Trainer",
      "description": "지수/환율/종목 예측 리포트 생성 에이전트",
      "isDefault": false,
      "supportedIntents": ["quant.forecast", "quant.general"],
      "allowedTools": ["quant.predict_market"]
    },
    {
      "key": "doc-rag",
      "name": "Document RAG",
      "description": "개인 문서(Notion RAG) 검색/요약 에이전트",
      "isDefault": false,
      "supportedIntents": ["rag.search", "rag.general"],
      "allowedTools": ["rag.search_documents"]
    }
  ],
  "correlationId": "..."
}
```

## RAG DB 설정
`doc-rag` 에이전트는 아래 설정으로 PostgreSQL `notion_rag` 스키마를 조회합니다.

- `AGENT_RAG_DB_URL` (기본: `jdbc:postgresql://localhost:5432/postgres`)
- `AGENT_RAG_DB_USERNAME` (기본: OS 사용자명)
- `AGENT_RAG_DB_PASSWORD` (기본: 빈값)
- `AGENT_RAG_SCHEMA` (기본: `notion_rag`)

툴:
- `rag.search_documents` 인자: `query(optional)`, `pathQuery(optional)`, `limit(optional)`

## Phase 3
- `.agent/work/api_collection.json` 기반 Daou Portal 실제 연동 추가
- Calendar / Mail / Messenger tool adapter 분리
- Quant Trainer 예측 도구(`quant.predict_market`) 추가
- Document RAG 조회 도구(`rag.search_documents`) 추가
- agent profile 기반 rule-based intent/allowed tool 제어 추가
- approval resume idempotency 및 context 검증 추가
- `X-Correlation-Id` 헤더, 구조화 로그, 응답 버전 관리 추가

## 클라이언트 문서
- [Frontend Agent Selection Guide](docs/frontend-agent-selection-guide.md)

## 작업 규칙
- `.agent/java-spring-ai-git-workflow-guide.md` 기준 협업
- `.agent/work/` 하위 작업 문서는 `.gitignore`로 제외
- PR 템플릿: `.github/pull_request_template.md`
