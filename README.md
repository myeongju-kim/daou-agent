# daouoffice-agent

Java/Spring 기반 Daouoffice Agent 프로젝트입니다.

## 기술 스택
- Java 25
- Spring Boot 4.0.3
- Spring AI 2.0.0-M2
- Gradle

## 시작하기
### 요구사항
- JDK 25
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

## Git/협업 규칙 요약
`.agent/java-spring-ai-git-workflow-guide.md` 기준으로 운영합니다.

### 브랜치 전략
- 기준 브랜치: `main` (필요 시 `develop` 통합 브랜치 사용)
- 작업 브랜치: `feature/*`, `fix/*`, `hotfix/*`, `refactor/*`, `chore/*`, `test/*`, `perf/*`
- AI 작업 브랜치: `ai/*`

브랜치 이름 형식:
```text
<type>/<ticket-or-scope>-<short-description>
```

예시:
```text
feature/OMS-123-order-create-api
fix/login-token-refresh
ai/OMS-781-fix-order-status-mapping
```

### 커밋 메시지 규칙
Conventional Commits 형식을 사용합니다.

```text
<type>(<scope>): <subject>
```

예시:
```text
feat(order): 주문 생성 API 추가
fix(auth): refresh token 검증 오류 수정
fix(order): 주문 상태 매핑 오류 수정 [AI]
```

### PR 규칙
- `main` 직접 push 금지
- 모든 변경은 PR로 병합
- PR 템플릿 사용
- 테스트/빌드 결과 기록

PR 제목 형식:
```text
[type] 작업 요약
[ai][type] 작업 요약
```

## 템플릿 위치
- PR 템플릿: `.github/pull_request_template.md`
- 이슈 템플릿: `.github/ISSUE_TEMPLATE/issue_template.md`
