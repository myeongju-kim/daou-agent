# Frontend Agent Selection Guide

## 1. 목적
- 프론트에서 사용자 선택 기반으로 `agentKey`를 전달하고,
- 백엔드는 해당 에이전트 규칙(rule-based intent + allowed tools)으로 처리한다.

## 2. 기본 개념
- `agentKey`: 어떤 에이전트를 사용할지 식별
- `intent`: 백엔드가 규칙으로 해석한 의도 (`ChatResponse.intent`에 반환)
- `allowedTools`: 해당 에이전트가 호출할 수 있는 tool 목록

중요:
- 백엔드는 허용되지 않은 도구 호출을 자동 차단한다.
- 차단 시 `/chat` 응답 `status=blocked`.

## 3. 권장 플로우
1. 앱 시작 시 `GET /agents` 호출
2. 목록을 에이전트 선택 UI(드롭다운/탭)에 표시
3. 사용자가 선택한 `agentKey`를 로컬 상태에 저장
4. `POST /chat` 호출 시 `agentKey` 포함
5. 응답의 `agentKey`, `intent`, `status`를 기반으로 UI 처리

## 4. API 예시

### 4.1 에이전트 목록
```http
GET /agents
```

응답(요약):
```json
{
  "version": "v1",
  "defaultAgentKey": "personal",
  "agents": [
    {
      "key": "personal",
      "name": "Personal Assistant",
      "supportedIntents": ["office.calendar", "office.mail", "office.messenger", "office.general"],
      "allowedTools": ["calendar.list_events", "mail.send_message", "messenger.send_message"]
    },
    {
      "key": "quant-trainer",
      "name": "Quant Trainer",
      "supportedIntents": ["quant.forecast", "quant.general"],
      "allowedTools": ["quant.predict_market"]
    }
  ]
}
```

### 4.2 채팅 요청
```http
POST /chat
Content-Type: application/json

{
  "sessionId": "room-001",
  "agentKey": "quant-trainer",
  "message": "엔비디아 1주일 전망 알려줘"
}
```

응답(요약):
```json
{
  "version": "v1",
  "status": "ok",
  "message": "...",
  "agentKey": "quant-trainer",
  "intent": "quant.forecast",
  "correlationId": "..."
}
```

### 4.3 허용되지 않은 도구 요청
`agentKey=quant-trainer` 상태에서 일정 생성 요청 시:

```json
{
  "version": "v1",
  "status": "blocked",
  "message": "선택한 에이전트에서 허용되지 않은 도구입니다: calendar.create_event",
  "agentKey": "quant-trainer",
  "intent": "quant.general"
}
```

## 5. 프론트 구현 포인트
- 세션 키 충돌 방지:
  - 같은 `sessionId`를 서로 다른 `agentKey`에서 재사용해도 백엔드가 내부적으로 분리 저장한다.
  - 프론트는 기존처럼 `sessionId` 문자열만 관리하면 된다.
- 권장 UI:
  - 상단 에이전트 선택 컴포넌트
  - 메시지 전송 시 현재 선택 `agentKey`를 항상 포함
  - `status=approval_required`면 승인 UI 노출
  - `status=blocked`면 에이전트 변경 또는 요청 수정 가이드 표시

## 6. 에러 처리 권장
- `BAD_REQUEST`: 잘못된 `agentKey` 혹은 요청 필드 오류
- `NOT_FOUND`: 존재하지 않는 세션/승인 요청
- `INTERNAL_ERROR`: 서버 내부 오류
- 모든 응답의 `correlationId`를 프론트 로그와 함께 저장해 장애 추적에 사용
