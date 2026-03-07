# daouoffice-agent

DaouOffice Desktop Agent용 Spring AI Backend MVP입니다.

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
- `GET /dashboard`
- `POST /approvals/{id}/approve`
- `POST /approvals/{id}/reject`

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
  "status": "approval_required",
  "message": "승인이 필요한 도구입니다: calendar.create_event",
  "steps": ["loop=1", "calendar.create_event"],
  "approvalId": "..."
}
```

## 작업 규칙
- `.agent/java-spring-ai-git-workflow-guide.md` 기준 협업
- `.agent/work/` 하위 작업 문서는 `.gitignore`로 제외
- PR 템플릿: `.github/pull_request_template.md`
