# Frontend Agent Selection Guide (Final)

## 1. 목적
- 프론트에서 사용자가 에이전트를 직접 선택하고, 선택한 `agentKey`에 맞는 룰 기반 처리 결과를 안정적으로 받는다.
- 백엔드는 `agentKey`별로 `intent`와 `allowedTools`를 강제한다.

## 2. 에이전트 카탈로그
- `personal`: 일정/메일/메신저 업무 자동화
- `quant-trainer`: 시장 전망 리포트
- `doc-rag`: 개인 문서(Notion RAG) 검색/근거 조회

중요:
- 프론트는 항상 canonical key(`personal`, `quant-trainer`, `doc-rag`)를 사용한다.
- 잘못된 조합(예: `quant-trainer`에서 일정 생성 요청)은 `status=blocked`로 응답된다.

## 3. 권장 플로우
1. 앱 초기 진입 시 `GET /agents` 호출
2. `agents[]`를 탭/드롭다운으로 노출
3. 사용자가 선택한 `agentKey`를 로컬 상태에 저장
4. `POST /chat` 요청마다 현재 `agentKey`를 포함
5. 응답의 `status`, `agentKey`, `intent`, `approval`를 기준으로 UI 분기

## 4. API 계약

### 4.1 에이전트 목록
```http
GET /agents
```

응답 예시:
```json
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
      "allowedTools": ["calendar.list_calendars", "calendar.list_events", "calendar.create_event", "mail.list_folders", "mail.list_messages", "mail.read_message", "mail.send_message", "messenger.send_message"]
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

### 4.2 채팅 요청
```http
POST /chat
Content-Type: application/json

{
  "sessionId": "room-001",
  "agentKey": "doc-rag",
  "message": "로그인 관련 정책 문서 찾아줘"
}
```

응답 예시:
```json
{
  "version": "v1",
  "status": "ok",
  "message": "...",
  "steps": ["loop=1", "tool_result"],
  "approvalId": "",
  "agentKey": "doc-rag",
  "intent": "rag.search",
  "correlationId": "..."
}
```

### 4.3 승인 필요 응답
`status=approval_required`일 때 `approval` 객체를 그대로 사용:
- `approval.reason`
- `approval.action`
- `approval.toolName`
- `approval.preview`
- `approval.arguments`

프론트는 이 정보를 확인 모달/시트로 노출하고,
- 승인: `POST /approvals/{id}/approve-and-resume`
- 거절: `POST /approvals/{id}/reject`
를 호출한다.

### 4.4 차단 응답
예: `agentKey=doc-rag`에서 메일 발송 요청
```json
{
  "version": "v1",
  "status": "blocked",
  "message": "선택한 에이전트에서 허용되지 않은 도구입니다: mail.send_message",
  "agentKey": "doc-rag",
  "intent": "rag.general"
}
```

## 5. 프론트 상태 처리 규칙
- `ok`: 메시지 렌더링
- `approval_required`: 승인 UI 오픈 후 사용자 액션 대기
- `blocked`: 현재 에이전트와 요청 불일치 안내 + 에이전트 전환 CTA
- `error`: `correlationId` 포함하여 에러 토스트/로그 저장

## 6. 세션 처리
- 동일한 `sessionId`를 에이전트별로 재사용해도 서버가 내부적으로 분리 저장한다.
- 프론트는 기존처럼 문자열 `sessionId`만 관리하면 된다.

## 7. RAG 에이전트 요청 가이드
- 사용자 입력 원문을 그대로 `message`로 전달한다.
- 별도 툴 파라미터를 프론트가 직접 구성할 필요는 없다.
- 검색 품질이 낮으면 사용자가 키워드를 명시하도록 UX 힌트 제공:
  - 예: "로그인", "권한", "배포", "온보딩"

## 8. 운영 권장 사항
- 모든 API 응답의 `correlationId`를 프론트 로그와 함께 저장
- `/agents` 응답은 앱 시작 시 1회 로드 후 캐시, 새로고침 시 재조회
- `allowedTools`는 UI 힌트(가능한 작업 안내)로만 사용하고, 실제 권한 판단은 서버 응답을 신뢰
