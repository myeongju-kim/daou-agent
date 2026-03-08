# MVP Client API Guide

DaouOffice Agent Backend MVP를 클라이언트(Electron/Web/CLI)에서 바로 호출할 수 있도록 정리한 문서입니다.

## 1) 기본 정보
- Base URL (local): `http://localhost:8080`
- Content-Type: `application/json`
- 인증: 현재 MVP에는 별도 인증 헤더 없음

## 2) 빠른 호출 순서
1. `POST /chat` 호출
2. 응답 `status` 확인
- `ok`: 결과 표시
- `approval_required`: `approvalId`를 사용해 승인/거절 API 호출
- `blocked` 또는 `error`: 메시지 표시
3. 승인 필요 시
- 승인: `POST /approvals/{id}/approve`
- 거절: `POST /approvals/{id}/reject`

주의: 현재 MVP는 승인 후 자동 재개(resume)까지는 구현되지 않았습니다. 승인 완료 후 클라이언트에서 다시 `POST /chat`을 호출하는 방식으로 처리합니다.

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
  "status": "approval_required",
  "message": "승인이 필요한 도구입니다: calendar.create_event",
  "steps": [
    "loop=1",
    "calendar.create_event"
  ],
  "approvalId": "3d2f3d70-7b3d-4f1a-9f28-6f5ad8456b42"
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
  "approvalId": "3d2f3d70-7b3d-4f1a-9f28-6f5ad8456b42",
  "status": "approved",
  "message": "승인 처리되었습니다."
}
```

## POST /approvals/{id}/reject

Response Body
```json
{
  "approvalId": "3d2f3d70-7b3d-4f1a-9f28-6f5ad8456b42",
  "status": "rejected",
  "message": "거절 처리되었습니다."
}
```

## 5) Dashboard API
## GET /dashboard
MVP 상태(세션/승인 대기/provider/tool 목록) 조회 API입니다.

Response Body
```json
{
  "sessionCount": 1,
  "pendingApprovalCount": 0,
  "llmProvider": "ollama",
  "tools": [
    "calendar.list_events",
    "calendar.create_event",
    "http.request",
    "fs.read"
  ]
}
```

## 6) 에러 응답 포맷
검증/비즈니스 오류는 아래 포맷을 사용합니다.

```json
{
  "status": "bad_request",
  "message": "요청 값 검증에 실패했습니다."
}
```

또는

```json
{
  "status": "error",
  "message": "내부 오류가 발생했습니다."
}
```

## 7) cURL 예시
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

### 대시보드 조회
```bash
curl http://localhost:8080/dashboard
```

## 8) 클라이언트 구현 팁
- `sessionId`는 클라이언트 대화 탭 단위로 고정 유지
- `approval_required` 수신 시 승인 모달을 띄우고 `approvalId` 저장
- `steps`는 디버그 패널에 노출하면 MVP 동작 추적에 유용
- 타임아웃은 클라이언트에서 10~15초 기준으로 우선 적용 권장
