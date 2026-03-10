# MVP Client API Guide

DaouOffice Agent Backend MVP를 클라이언트(Electron/Web/CLI)에서 바로 호출할 수 있도록 정리한 문서입니다.

## 1) 기본 정보
- Base URL (local): `http://localhost:8080`
- Content-Type: `application/json`
- 인증: 현재 MVP에는 별도 인증 헤더 없음
- 응답 공통 필드: `version`, `correlationId`
- 응답 헤더: `X-Correlation-Id`

## 2) 빠른 호출 순서
1. `POST /chat` 호출
2. 응답 `status` 확인
- `ok`: 결과 표시
- `approval_required`: `approvalId`를 사용해 승인/거절 API 호출
- `blocked` 또는 `error`: 메시지 표시
3. 승인 필요 시
- 승인: `POST /approvals/{id}/approve`
- 거절: `POST /approvals/{id}/reject`
- 승인+재개: `POST /approvals/{id}/approve-and-resume`
4. Ollama 사용 시 모델 선택 저장
- 모델 목록 조회: `GET /ollama/models`
- 세션 모델 저장: `POST /ollama/models/select`
- 세션 저장 모델 조회: `GET /ollama/models/select/{sessionId}`

권장: 승인 이후 바로 응답을 이어받으려면 `approve-and-resume` API를 사용합니다.

## 3) Chat API
## POST /chat
사용자 메시지를 세션 단위로 처리하고 Agent 결과를 반환합니다.

Request Body
```json
{
  "sessionId": "default",
  "message": "내일 오전 10시에 일정 등록해줘"
}
```

Response Body
```json
{
  "version": "v1",
  "status": "approval_required",
  "message": "승인이 필요한 도구입니다: calendar.create_event",
  "steps": [
    "loop=1",
    "calendar.create_event"
  ],
  "approvalId": "3d2f3d70-7b3d-4f1a-9f28-6f5ad8456b42",
  "correlationId": "a4abf147-0d9a-4b1b-b17b-6f9ebd6ef10d"
}
```

### status 값
- `ok`: 정상 완료
- `approval_required`: 승인 필요
- `blocked`: 정책상 차단
- `error`: 내부 처리 오류

## 4) Approval API
## POST /approvals/{id}/approve

Response Body
```json
{
  "version": "v1",
  "approvalId": "3d2f3d70-7b3d-4f1a-9f28-6f5ad8456b42",
  "status": "approved",
  "message": "승인 처리되었습니다.",
  "correlationId": "a4abf147-0d9a-4b1b-b17b-6f9ebd6ef10d"
}
```

## POST /approvals/{id}/reject

Response Body
```json
{
  "version": "v1",
  "approvalId": "3d2f3d70-7b3d-4f1a-9f28-6f5ad8456b42",
  "status": "rejected",
  "message": "거절 처리되었습니다.",
  "correlationId": "a4abf147-0d9a-4b1b-b17b-6f9ebd6ef10d"
}
```

## POST /approvals/{id}/approve-and-resume
승인 처리 후 대기 중이던 Tool 실행을 이어서 재개하고, `POST /chat`과 동일한 응답 형식으로 결과를 반환합니다.

Response Body
```json
{
  "version": "v1",
  "status": "ok",
  "message": "요청이 완료되었습니다.",
  "steps": [
    "3d2f3d70-7b3d-4f1a-9f28-6f5ad8456b42",
    "calendar.create_event: 일정 생성 요청이 접수되었습니다.",
    "loop=1"
  ],
  "approvalId": "",
  "correlationId": "a4abf147-0d9a-4b1b-b17b-6f9ebd6ef10d"
}
```

## 5) Dashboard API
## GET /dashboard
MVP 상태(세션/승인 대기/provider/tool 목록) 조회 API입니다.

Response Body
```json
{
  "version": "v1",
  "sessionCount": 1,
  "pendingApprovalCount": 0,
  "llmProvider": "ollama",
  "tools": [
    "calendar.list_calendars",
    "calendar.list_events",
    "calendar.create_event",
    "mail.list_folders",
    "mail.list_messages",
    "mail.read_message",
    "mail.send_message",
    "messenger.send_message",
    "http.request",
    "fs.read"
  ],
  "correlationId": "a4abf147-0d9a-4b1b-b17b-6f9ebd6ef10d"
}
```

## 6) Ollama Model API
## GET /ollama/models
`agent.llm-provider=ollama` 인 경우 Ollama 서버에 등록된 모델 목록을 조회합니다.

Response Body
```json
{
  "provider": "ollama",
  "modelCount": 2,
  "models": [
    {
      "name": "qwen2.5:7b",
      "model": "qwen2.5:7b",
      "modifiedAt": "2026-03-08T10:12:00Z",
      "size": 4653242378,
      "digest": "sha256:...",
      "family": "qwen2",
      "parameterSize": "7B",
      "quantizationLevel": "Q4_0"
    }
  ]
}
```

## POST /ollama/models/select
사용자가 선택한 모델을 세션 기준으로 저장합니다. 이후 `POST /chat` 호출은 해당 세션의 저장 모델로 실행됩니다.

Request Body
```json
{
  "sessionId": "default",
  "model": "qwen2.5:7b"
}
```

Response Body
```json
{
  "provider": "ollama",
  "sessionId": "default",
  "model": "qwen2.5:7b",
  "message": "선택한 모델이 세션에 저장되었습니다."
}
```

## GET /ollama/models/select/{sessionId}
세션에 저장된 현재 모델을 조회합니다.

Response Body
```json
{
  "provider": "ollama",
  "sessionId": "default",
  "model": "qwen2.5:7b",
  "message": "현재 세션의 선택 모델입니다."
}
```

## 7) 에러 응답 포맷
검증/비즈니스 오류는 아래 포맷을 사용합니다.

```json
{
  "version": "v1",
  "status": "bad_request",
  "errorCode": "VALIDATION_ERROR",
  "message": "요청 값 검증에 실패했습니다.",
  "correlationId": "a4abf147-0d9a-4b1b-b17b-6f9ebd6ef10d"
}
```

또는

```json
{
  "version": "v1",
  "status": "error",
  "errorCode": "INTERNAL_ERROR",
  "message": "내부 오류가 발생했습니다.",
  "correlationId": "a4abf147-0d9a-4b1b-b17b-6f9ebd6ef10d"
}
```

## 8) cURL 예시
### chat 호출
```bash
curl -X POST http://localhost:8080/chat \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"default","message":"오늘 일정 알려줘"}'
```

### 승인 처리
```bash
curl -X POST http://localhost:8080/approvals/{approvalId}/approve
```

### 승인 후 재개
```bash
curl -X POST http://localhost:8080/approvals/{approvalId}/approve-and-resume
```

### 대시보드 조회
```bash
curl http://localhost:8080/dashboard
```

### Ollama 모델 조회
```bash
curl http://localhost:8080/ollama/models
```

### Ollama 모델 선택 저장
```bash
curl -X POST http://localhost:8080/ollama/models/select \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"default","model":"qwen2.5:7b"}'
```

### 세션 선택 모델 조회
```bash
curl http://localhost:8080/ollama/models/select/default
```

## 9) 클라이언트 구현 팁
- `sessionId`는 클라이언트 대화 탭 단위로 고정 유지
- `approval_required` 수신 시 승인 모달을 띄우고 `approvalId` 저장
- `approve-and-resume`는 동일 `approvalId` 재호출 시 동일 결과를 반환하도록 idempotent 처리됩니다.
- `correlationId`와 `X-Correlation-Id`를 Electron 로그에 함께 남기면 서버 추적이 쉬워집니다.
- `steps`는 디버그 패널에 노출하면 MVP 동작 추적에 유용
- 타임아웃은 클라이언트에서 10~15초 기준으로 우선 적용 권장

## 10) Phase 3 Tool 범위
- `calendar.list_calendars`
- `calendar.list_events`
- `calendar.create_event`
- `mail.list_folders`
- `mail.list_messages`
- `mail.read_message`
- `mail.send_message`
- `messenger.send_message`
- `http.request`

`calendar/*`, `mail/*`, `messenger/*` 는 `.agent/work/api_collection.json` 기준 Daou Portal OpenAPI v2 endpoint로 실제 연결됩니다.

민감정보(`client_secret`, `username`, `password`)는 `application.yaml`에 직접 커밋하지 않고 환경변수로 주입해야 합니다.
