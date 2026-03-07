# Java Spring 프로젝트용 브랜치 전략 / 커밋 메시지 규칙 / AI 자동 Commit·Push·PR 생성 가이드

## 1. 문서 목적
이 문서는 **Java Spring 기반 프로젝트**에서 사용할 수 있는 표준 Git 협업 규칙과, **AI Agent(Codex, Claude Code, 사내 MCP Agent 등)** 가 안전하게 **브랜치 생성 → 코드 수정 → 커밋 → 푸시 → PR 생성** 까지 수행할 수 있도록 하는 운영 기준을 정의한다.

이 문서는 다음 목표를 가진다.

- 사람과 AI가 **동일한 Git 규칙**을 사용한다.
- 브랜치명만 봐도 **작업 목적이 즉시 드러난다.**
- 커밋 메시지만 봐도 **무슨 변경인지 추적 가능하다.**
- AI가 자동으로 작업하더라도 **리뷰 가능성과 감사 가능성**을 유지한다.
- `main` 보호, 브랜치 분리, PR 템플릿 기반으로 **운영 안정성**을 확보한다.

---

## 2. 기본 원칙

### 2.1 운영 원칙
1. `main` 브랜치에는 직접 push 하지 않는다.
2. 모든 개발 작업은 **작업 브랜치**에서 수행한다.
3. 사람 작업과 AI 작업 모두 **PR(Pull Request)** 을 통해 병합한다.
4. AI는 원칙적으로 `main` 에 직접 반영하지 않고, **브랜치 단위 결과물**만 생성한다.
5. AI 커밋은 반드시 **작업 목적, 범위, 위험도**가 추적 가능해야 한다.
6. PR 생성 전 최소한의 **빌드/테스트/정적 점검**을 수행한다.
7. 긴급 핫픽스를 제외하면 rebase/squash 기준은 팀 정책에 맞추되, **최종 병합 히스토리는 읽기 쉬워야 한다.**

### 2.2 권장 보호 정책
- `main` 브랜치 보호
- PR 없이 merge 금지
- 최소 1명 이상 승인 후 merge
- CI 성공 시에만 merge 허용
- force push 금지
- stale branch 자동 정리 권장

---

## 3. 추천 브랜치 전략
이 문서는 **GitHub Flow + 운영 보완 규칙** 기반 전략을 권장한다.

Spring 프로젝트는 백엔드 API, 배치, 어드민, 공통 모듈 수정이 잦고 배포 단위가 비교적 명확하므로, 지나치게 무거운 Git Flow보다 다음 구조가 실무 운영에 적합하다.

### 3.1 기본 브랜치

```text
main        : 운영 반영 기준 브랜치
```

필요 시 아래를 추가할 수 있다.

```text
develop     : 통합 검증용 브랜치 (선택)
main        : 운영 반영 기준 브랜치
```

단, 팀 규모가 크지 않거나 배포 빈도가 높다면 `develop` 없이 `main` 중심으로 운영하는 것이 단순하고 관리가 쉽다.

### 3.2 작업 브랜치 종류

```text
feature/    : 신규 기능
fix/        : 일반 버그 수정
hotfix/     : 운영 긴급 장애 수정
refactor/   : 구조 개선, 리팩토링
chore/      : 설정, 문서, 빌드, 의존성, 운영성 작업
test/       : 테스트 코드 보강
perf/       : 성능 개선
ai/         : AI가 수행한 작업 브랜치
```

### 3.3 브랜치 네이밍 규칙

형식:

```text
<type>/<ticket-or-scope>-<short-description>
```

예시:

```text
feature/order-create-api
feature/OMS-123-order-create-api
fix/login-token-refresh
hotfix/payment-null-pointer
refactor/member-service-split
chore/gradle-java21-upgrade
test/order-service-unit-test
perf/product-search-query
ai/OMS-781-fix-order-status-mapping
ai/refactor-payment-domain
```

### 3.4 브랜치명 작성 규칙
- 소문자 사용
- 공백 사용 금지
- 단어 구분은 `-` 사용
- 가능하면 이슈 번호 또는 업무 번호 포함
- 너무 긴 브랜치명 금지
- 의미 없는 이름 금지 (`test`, `work`, `temp`, `final`, `real-final` 등)

잘못된 예:

```text
feature/test
feature/final
fix/aaa
mybranch
작업브랜치
```

좋은 예:

```text
feature/user-password-reset
fix/OMS-912-order-cancel-condition
refactor/external-api-client-separation
ai/CP-221-fix-coupon-expire-batch
```

---

## 4. AI 전용 브랜치 전략
AI가 자동 작업하는 경우, 일반 브랜치와 구분되도록 **`ai/` prefix** 사용을 권장한다.

### 4.1 AI 브랜치 네이밍 규칙

형식:

```text
ai/<ticket-or-scope>-<short-description>
```

예시:

```text
ai/OMS-1201-add-order-search-condition
ai/WMS-88-fix-stock-sync-null-check
ai/refactor-common-response-handler
ai/chore-update-springdoc-config
```

### 4.2 AI 브랜치 운영 원칙
1. AI는 항상 새 브랜치를 생성한 뒤 작업한다.
2. 같은 브랜치를 여러 작업이 재사용하지 않는다.
3. AI 브랜치는 작업 단위를 작게 유지한다.
4. AI는 브랜치 생성 시 기준 브랜치를 명확히 한다. 기본은 `main` 이다.
5. AI는 push 후 반드시 PR을 생성한다.
6. AI는 CI 실패 시 실패 이유를 PR description 또는 코멘트에 남긴다.

### 4.3 추천 기본 흐름

```text
main checkout
→ 최신화 pull
→ ai/... 브랜치 생성
→ 코드 수정
→ build/test 수행
→ commit
→ push
→ PR 생성
```

---

## 5. 커밋 메시지 규칙
이 문서는 **Conventional Commits 기반 + 한국어 설명 허용** 규칙을 권장한다.

### 5.1 기본 형식

```text
<type>(<scope>): <subject>
```

scope 가 불필요하면 생략 가능하다.

```text
<type>: <subject>
```

예시:

```text
feat(order): 주문 생성 API 추가
fix(auth): refresh token 검증 오류 수정
refactor(member): 회원 서비스 책임 분리
chore(gradle): Java 21 설정 반영
test(order): 주문 상태 전이 테스트 추가
perf(search): 상품 조회 인덱스 조건 최적화
```

### 5.2 타입 목록

```text
feat      : 신규 기능
fix       : 버그 수정
refactor  : 동작 변경 없는 구조 개선
chore     : 빌드/설정/문서/운영성 작업
style     : 포맷팅, 세미콜론, import 정리 등
build     : Gradle/Maven/패키징 변경
test      : 테스트 추가/수정
perf      : 성능 개선
docs      : 문서 수정
ci        : CI/CD 설정 변경
revert    : 이전 커밋 되돌림
```

### 5.3 subject 작성 규칙
- 현재형 사용
- 한 줄로 간결하게 작성
- 불필요한 마침표 생략
- `무엇을` 변경했는지 드러나게 작성
- 가능하면 `왜`가 유추되도록 작성

좋지 않은 예:

```text
fix: 수정
feat: 개발
refactor: 변경
chore: 작업
```

좋은 예:

```text
fix(order): 주문 상세 조회 시 null 옵션 매핑 오류 수정
feat(member): 휴대폰 본인인증 이력 조회 API 추가
refactor(payment): 결제 승인 처리 로직을 PaymentProcessor로 분리
chore(logging): MDC traceId 출력 패턴 반영
```

### 5.4 본문(body) 규칙
필요 시 커밋 본문에 아래 항목을 추가한다.

```text
- 변경 이유
- 주요 변경 내용
- 영향 범위
- 테스트 여부
```

예시:

```text
fix(order): 주문 취소 시 재고 복원 누락 수정

- 주문 취소 완료 분기에서 재고 복원 이벤트가 누락되던 문제 수정
- OrderCancelService -> StockRestorePublisher 호출 추가
- 주문 취소/재고 복원 연계 로직 테스트 보강
- 주문/재고 도메인에 영향
```

### 5.5 Breaking Change 표기
하위 호환이 깨지는 변경은 아래처럼 명시한다.

```text
feat(api)!: 주문 응답 스키마를 v2 형식으로 변경
```

또는 본문에:

```text
BREAKING CHANGE: order response 필드명이 변경됨
```

---

## 6. AI 전용 커밋 메시지 규칙
AI가 만든 커밋은 일반 커밋과 동일한 규칙을 따르되, **AI 작업임을 식별 가능**하게 하는 규칙을 추가한다.

### 6.1 추천 형식 A: 일반 규칙 유지 + PR에서 AI 명시
가장 깔끔한 방식이다.

```text
fix(order): 주문 상태 매핑 오류 수정
```

이 경우 AI 여부는 다음으로 구분한다.
- AI 전용 브랜치명 (`ai/...`)
- AI 전용 Git author
- PR 템플릿 내 AI 작업 표기

### 6.2 추천 형식 B: subject 뒤에 `[AI]` 추가
조직에서 히스토리 식별이 중요하면 아래 형식도 가능하다.

```text
fix(order): 주문 상태 매핑 오류 수정 [AI]
refactor(payment): 결제 도메인 책임 분리 [AI]
```

### 6.3 비추천 형식
아래처럼 메시지 자체가 모호한 형태는 금지한다.

```text
ai: fix code
ai: update files
fix: by ai
chore: codex change
```

---

## 7. PR 제목 규칙
PR 제목도 커밋 메시지와 유사한 규칙을 사용한다.

### 7.1 기본 형식

```text
[type] 작업 요약
```

예시:

```text
[feat] 주문 생성 API 추가
[fix] 로그인 토큰 갱신 오류 수정
[refactor] 회원 서비스 책임 분리
[chore] Spring Boot 3.5 설정 정리
[ai][fix] 주문 상태 매핑 오류 수정
```

### 7.2 AI 작업 PR 제목 예시

```text
[ai][feat] 주문 검색 조건 API 추가
[ai][fix] 재고 동기화 null 체크 보완
[ai][refactor] 결제 승인 로직 책임 분리
```

---

## 8. PR 본문 템플릿
아래 템플릿을 그대로 사용하면 된다.

```md
## 작업 유형
- [ ] feat
- [ ] fix
- [ ] refactor
- [ ] chore
- [ ] test
- [ ] perf

## 작업 주체
- [ ] Human
- [ ] AI

## 작업 배경
- 왜 이 작업이 필요한지 작성

## 주요 변경 사항
- 변경 사항 1
- 변경 사항 2
- 변경 사항 3

## 영향 범위
- 주문
- 회원
- 결제
- 배치
- 공통

## 테스트/검증
- [ ] Gradle build 통과
- [ ] 테스트 통과
- [ ] 로컬 실행 확인
- [ ] 정적 분석 확인

## 체크리스트
- [ ] main 직접 수정 아님
- [ ] 불필요한 파일 변경 없음
- [ ] 민감정보 포함 없음
- [ ] 로그/예외 처리 점검
- [ ] 리뷰 가능 단위 유지

## 참고
- 관련 이슈: 
- 관련 문서: 
```

---

## 9. Java Spring 프로젝트 권장 브랜치 전략 상세

### 9.1 일반 기능 개발

```text
main
  └─ feature/OMS-101-order-create-api
```

흐름:
1. `main` 최신화
2. `feature/...` 브랜치 생성
3. 개발 진행
4. 테스트 수행
5. PR 생성
6. 리뷰 후 merge

### 9.2 일반 버그 수정

```text
main
  └─ fix/OMS-212-order-status-bug
```

### 9.3 긴급 운영 장애

```text
main
  └─ hotfix/OMS-999-payment-timeout-fix
```

핫픽스는 빠르게 반영하되, 다음 원칙을 지킨다.
- 범위를 최소화한다.
- 원인/영향 범위를 PR에 명확히 적는다.
- 배포 후 회고 및 재발 방지 항목을 남긴다.

### 9.4 리팩토링

```text
main
  └─ refactor/member-service-layer-split
```

리팩토링은 반드시 아래를 만족해야 한다.
- 기능 스펙 변경 없음
- 테스트 보강 또는 기존 테스트 통과
- 변경 범위가 과도하게 크지 않음

### 9.5 AI 작업 브랜치

```text
main
  └─ ai/OMS-321-fix-order-cancel-rollback
```

AI 작업은 다음 기준을 만족해야 한다.
- 단일 목적
- 작은 변경 단위
- 컴파일 가능한 상태 유지
- PR 설명에 변경 범위 명시

---

## 10. Git Author 전략
AI가 커밋했을 때 Git 히스토리에서 식별 가능하도록 **전용 bot 계정** 또는 **전용 author** 사용을 권장한다.

### 10.1 권장 방식

```bash
git config user.name "mj-ai-bot"
git config user.email "mjoo1106@khu.ac.kr"
```

또는 커밋 시 일회성 author 지정:

```bash
git commit --author="mj-ai-bot <mjoo1106@khu.ac.kr>" -m "fix(order): 주문 상태 매핑 오류 수정 [AI]"
```

### 10.2 권장 이유
- 누가 작업했는지 히스토리에서 구분 가능
- 사람 커밋과 AI 커밋 혼선 감소
- 감사 로그 추적 용이
- AI 자동화 도입 시 조직 설득이 쉬움

### 10.3 AI Commit/Push 계정 및 PAT 운영 정책
AI가 `git commit`, `git push`, `gh` 명령을 사용할 때는 **동일한 전용 GitHub 계정** 과 **해당 계정 PAT** 를 사용한다.

- AI 자동화 Commit/Push는 지정된 전용 계정만 사용
- commit author 계정과 push 인증 계정은 반드시 동일해야 함
- PAT는 문서/코드/스크립트에 평문 저장 금지
- PAT는 CI Secret 또는 로컬 환경변수(`GITHUB_TOKEN`)로만 주입
- 토큰이 노출되면 즉시 폐기(revoke) 후 재발급

예시(로컬/CI 공통, 계정 식별 + PAT 주입):

```bash
git config user.name "mj-ai-bot"
git config user.email "mjoo1106@khu.ac.kr"
export GITHUB_TOKEN="<PAT>"
gh auth login --with-token <<< "$GITHUB_TOKEN"
```

선택적으로 HTTPS Push에 토큰을 사용하는 경우:

```bash
git remote set-url origin https://x-access-token:${GITHUB_TOKEN}@github.com/<owner>/<repo>.git
```

로컬 저장소에서 `GITHUB_TOKEN` 환경변수를 사용하는 credential helper 예시:

```bash
git config credential.helper '!f() { \
  test -n "$GITHUB_TOKEN" || { echo "GITHUB_TOKEN is not set" >&2; exit 1; }; \
  echo "username=x-access-token"; \
  echo "password=$GITHUB_TOKEN"; \
}; f'
```

---

## 11. AI 자동 작업 표준 플로우
이 섹션은 AI Agent 가 실제로 수행해야 할 표준 절차다.

### 11.1 전체 플로우

```text
1. 기준 브랜치 확인
2. 최신 코드 pull
3. 새 작업 브랜치 생성
4. 요구사항 범위 확인
5. 코드 수정
6. 테스트/빌드/정적 점검
7. 변경 파일 검토
8. 커밋 메시지 생성
9. commit
10. push
11. PR 생성
12. PR 본문 작성
13. 실패/주의사항 기록
```

### 11.2 단계별 상세

#### 1) 기준 브랜치 확인
기본 기준 브랜치는 `main` 으로 한다.

```bash
git checkout main
git pull origin main
```

#### 2) 작업 브랜치 생성

```bash
git checkout -b ai/OMS-321-fix-order-cancel-rollback
```

#### 3) 코드 수정
- 요구사항 범위 밖 수정 금지
- 포맷팅 전체 파일 변경 금지
- 민감정보 추가 금지
- 불필요한 import/설정 변경 최소화

#### 4) 빌드/테스트 수행
Gradle 기준:

```bash
./gradlew clean test
./gradlew build
```

필요 시 모듈 단위:

```bash
./gradlew :api:test
./gradlew :batch:test
```

#### 5) 변경 파일 검토

```bash
git status
git diff --stat
git diff
```

검토 포인트:
- 의도하지 않은 파일 변경 없는가
- `application.yml`, secret, token 유출 없는가
- 로그/디버깅 코드 제거됐는가
- import 정리만 과도하게 발생하지 않았는가

#### 6) 커밋

```bash
git add .
git commit -m "fix(order): 주문 취소 롤백 처리 누락 수정 [AI]"
```

#### 7) Push

```bash
git push origin ai/OMS-321-fix-order-cancel-rollback
```

#### 8) PR 생성
GitHub CLI 기준:

```bash
gh pr create \
  --base main \
  --head ai/OMS-321-fix-order-cancel-rollback \
  --title "[ai][fix] 주문 취소 롤백 처리 누락 수정" \
  --body-file .github/pull_request_template.md
```

---

## 12. AI 실행 가드레일
AI가 commit/push/PR 까지 하더라도 반드시 아래 규칙을 지켜야 한다.

### 12.1 금지 사항
- `main` 직접 push 금지
- force push 금지
- 비관련 파일 대량 수정 금지
- 보안 설정 임의 변경 금지
- 비밀번호/토큰/API key 커밋 금지
- 테스트 실패 상태에서 PR 생성 금지 (예외 시 이유 명시)
- schema/data migration 포함 시 명시 없이 반영 금지

### 12.2 필수 확인 사항
- 브랜치명 규칙 준수
- 커밋 메시지 규칙 준수
- 빌드/테스트 결과 기록
- 영향 범위 명시
- 리뷰 포인트 명시

### 12.3 권장 사항
- 한 PR 한 목적 유지
- 큰 작업은 여러 PR로 분리
- 리팩토링과 기능 변경 혼합 금지
- AI가 확신 없는 변경은 PR 본문에 위험 요소 명시

---

## 13. GitHub 저장소 설정 권장안

### 13.1 Branch Protection
`main` 에 대해 다음 설정 권장:

- Require a pull request before merging
- Require approvals
- Dismiss stale approvals when new commits are pushed
- Require status checks to pass before merging
- Require branches to be up to date before merging
- Restrict who can push to matching branches
- Do not allow force pushes

### 13.2 PR Template
`.github/pull_request_template.md`

```md
## 작업 유형
- [ ] feat
- [ ] fix
- [ ] refactor
- [ ] chore
- [ ] test
- [ ] perf

## 작업 주체
- [ ] Human
- [ ] AI

## 작업 배경
- 

## 주요 변경 사항
- 
- 
- 

## 영향 범위
- 

## 테스트/검증
- [ ] ./gradlew test
- [ ] ./gradlew build
- [ ] 정적 분석 확인

## 위험 요소 / 리뷰 포인트
- 

## 참고
- 관련 이슈:
- 관련 문서:
```

---

## 14. Java Spring 프로젝트용 실전 예시

### 14.1 신규 API 개발
브랜치:

```text
feature/OMS-101-order-create-api
```

커밋:

```text
feat(order): 주문 생성 API 추가
```

PR 제목:

```text
[feat] 주문 생성 API 추가
```

### 14.2 버그 수정
브랜치:

```text
fix/OMS-212-order-status-bug
```

커밋:

```text
fix(order): 주문 상태 코드 매핑 오류 수정
```

PR 제목:

```text
[fix] 주문 상태 코드 매핑 오류 수정
```

### 14.3 리팩토링
브랜치:

```text
refactor/payment-service-separation
```

커밋:

```text
refactor(payment): 결제 승인 책임을 PaymentProcessor로 분리
```

PR 제목:

```text
[refactor] 결제 승인 책임 분리
```

### 14.4 AI 자동 수정
브랜치:

```text
ai/OMS-321-fix-order-cancel-rollback
```

커밋:

```text
fix(order): 주문 취소 롤백 처리 누락 수정 [AI]
```

PR 제목:

```text
[ai][fix] 주문 취소 롤백 처리 누락 수정
```

---

## 15. AI Agent 작업 지시용 표준 프롬프트
아래 프롬프트는 Codex, Claude Code, 사내 MCP Agent 에 그대로 전달할 수 있는 작업 지시 템플릿이다.

```md
# AI Git 작업 지시

너는 Java Spring 프로젝트에서 작업하는 AI 개발 에이전트다.
아래 Git 규칙을 반드시 지켜라.

## 목표
- 요구사항 범위 내에서만 코드 수정
- 새 작업 브랜치 생성
- 빌드/테스트 수행
- 규칙에 맞는 커밋 메시지 작성
- 원격 브랜치 push
- PR 생성

## 브랜치 규칙
- main 직접 수정 금지
- 브랜치 형식: ai/<ticket-or-scope>-<short-description>
- 예: ai/OMS-321-fix-order-cancel-rollback

## 커밋 메시지 규칙
- 형식: <type>(<scope>): <subject> [AI]
- 예: fix(order): 주문 취소 롤백 처리 누락 수정 [AI]
- type 허용값: feat, fix, refactor, chore, test, perf, docs, ci, build

## PR 제목 규칙
- 형식: [ai][type] 작업 요약
- 예: [ai][fix] 주문 취소 롤백 처리 누락 수정

## 필수 절차
1. main checkout 및 최신화
2. ai 브랜치 생성
3. 요구사항 범위 내 수정
4. ./gradlew test 또는 가능한 최소 테스트 수행
5. ./gradlew build 가능 시 수행
6. git diff 검토
7. commit
8. push
9. PR 생성

## 금지 사항
- main 직접 push 금지
- force push 금지
- 비밀값 커밋 금지
- 요구사항 범위 밖 대규모 리팩토링 금지
- 테스트 실패 상태를 숨기지 말 것

## PR 본문에 반드시 포함할 것
- 작업 배경
- 주요 변경 사항
- 영향 범위
- 테스트 결과
- 위험 요소 / 리뷰 포인트
```

---

## 16. GitHub CLI 기반 자동화 예시 스크립트
아래 스크립트는 사람이 실행해도 되고, AI Agent 가 내부적으로 호출해도 된다.

파일명 예시: `ai-git-flow.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE_BRANCH="main"
BRANCH_NAME="$1"
COMMIT_MESSAGE="$2"
PR_TITLE="$3"
PR_BODY_FILE="${4:-.github/pull_request_template.md}"

if [[ -z "$BRANCH_NAME" || -z "$COMMIT_MESSAGE" || -z "$PR_TITLE" ]]; then
  echo "Usage: ./ai-git-flow.sh <branch-name> <commit-message> <pr-title> [pr-body-file]"
  exit 1
fi

echo "[1/8] checkout base branch"
git checkout "$BASE_BRANCH"

echo "[2/8] pull latest"
git pull origin "$BASE_BRANCH"

echo "[3/8] create working branch"
git checkout -b "$BRANCH_NAME"

echo "[4/8] please apply code changes before continuing"
read -p "Code changes applied? (y/N): " ANSWER
if [[ "$ANSWER" != "y" && "$ANSWER" != "Y" ]]; then
  echo "aborted"
  exit 1
fi

echo "[5/8] run tests"
./gradlew test || true

echo "[6/8] review changes"
git status
git diff --stat

echo "[7/8] commit"
git add .
git commit -m "$COMMIT_MESSAGE"

echo "[8/8] push and create pr"
git push -u origin "$BRANCH_NAME"
gh pr create --base "$BASE_BRANCH" --head "$BRANCH_NAME" --title "$PR_TITLE" --body-file "$PR_BODY_FILE"

echo "done"
```

---

## 17. Codex / Claude / MCP 용 툴 설계 권장안
AI가 완전 자동으로 처리하려면 Git 관련 Tool 을 다음처럼 분리하는 것이 좋다.

### 17.1 추천 Tool 목록

```text
git.checkout_base
git.pull
git.create_branch
git.status
git.diff
git.add
git.commit
git.push
git.create_pr
gradle.test
gradle.build
```

### 17.2 권장 실행 순서

```text
git.checkout_base
→ git.pull
→ git.create_branch
→ code edit
→ gradle.test
→ gradle.build
→ git.status
→ git.diff
→ git.add
→ git.commit
→ git.push
→ git.create_pr
```

### 17.3 MCP 서버 레벨 검증 포인트
- 브랜치명 prefix 검증 (`ai/`, `feature/`, `fix/` 등)
- `main` push 차단
- force push 차단
- 커밋 메시지 형식 검증
- PR 제목 형식 검증
- 테스트 실패 시 경고 또는 PR 본문 강제 기록

---

## 18. 저장소에 바로 넣을 수 있는 정책 문구 예시
아래 내용은 `CONTRIBUTING.md` 또는 `docs/git-policy.md` 로 넣을 수 있다.

```md
## Branch Policy
- main direct push is prohibited.
- All changes must be made through a pull request.
- Branch naming must follow: <type>/<ticket-or-scope>-<short-description>
- AI-generated changes must use ai/ prefix.

## Commit Message Policy
- Commit messages must follow Conventional Commits.
- Format: <type>(<scope>): <subject>
- Example: fix(order): 주문 상태 매핑 오류 수정
- AI-generated commits may append [AI].

## Pull Request Policy
- PR title format: [type] 작업 요약
- AI-generated PR title format: [ai][type] 작업 요약
- PR body must include background, changes, impact, test result, and review points.
```

---

## 19. 최종 권장 운영안
명확하고 실무적인 기본안은 아래와 같다.

### 19.1 브랜치 전략
- 기준 브랜치: `main`
- 작업 브랜치: `feature/`, `fix/`, `hotfix/`, `refactor/`, `chore/`, `test/`, `perf/`
- AI 브랜치: `ai/...`

### 19.2 커밋 전략
- Conventional Commits 사용
- 한글 subject 허용
- AI 커밋은 `[AI]` 표기 선택 적용

### 19.3 PR 전략
- PR 제목 규칙 통일
- PR 템플릿 사용
- CI 통과 후 리뷰
- `main` 직접 반영 금지

### 19.4 AI 자동화 전략
- AI는 브랜치 생성부터 PR 생성까지 수행 가능
- 단, `main` push 금지
- 테스트 결과와 영향 범위를 반드시 남김
- Git author 또는 bot 계정으로 AI 이력 구분

---

## 20. 추천 기본안 한 장 요약

```text
[브랜치]
main
feature/*
fix/*
hotfix/*
refactor/*
chore/*
test/*
perf/*
ai/*

[브랜치명]
<type>/<ticket-or-scope>-<short-description>
예) ai/OMS-321-fix-order-cancel-rollback

[커밋]
<type>(<scope>): <subject>
예) fix(order): 주문 취소 롤백 처리 누락 수정 [AI]

[PR 제목]
[type] 작업 요약
[ai][type] 작업 요약
예) [ai][fix] 주문 취소 롤백 처리 누락 수정

[AI 금지사항]
- main 직접 push 금지
- force push 금지
- 민감정보 커밋 금지
- 테스트 실패 숨김 금지
```

---

## 21. 결론
이 문서 기준으로 운영하면 다음이 가능하다.

- Java Spring 프로젝트에 맞는 **일관된 브랜치 전략** 정립
- 사람이 읽기 쉬운 **커밋 메시지 규칙** 통일
- AI가 **커밋/푸시/PR 생성** 까지 수행하는 자동화 체계 구축
- 히스토리, 책임 주체, 영향 범위가 분명한 **감사 가능한 개발 프로세스** 확보

AI 자동화의 핵심은 "AI가 얼마나 많이 수정하느냐" 가 아니라, **얼마나 안전한 규칙 안에서 수정하느냐** 다.
이 문서의 목적은 바로 그 운영 기준을 만드는 것이다.
