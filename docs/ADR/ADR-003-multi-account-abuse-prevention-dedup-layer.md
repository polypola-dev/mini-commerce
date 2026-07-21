# ADR-003: 신규가입/혜택 어뷰징 방지를 위한 본인 식별 Dedup 레이어

- 상태: 제안 (2026-06-19)

## 컨텍스트

현재 인증은 Supabase Auth(이메일/비밀번호, 구글 OAuth, 네이버 Custom OAuth)만 사용한다.

Supabase의 계정 자동 연결(identity linking)은 "이메일 문자열이 정확히 일치"할 때만 동작한다. `auth.users.email`에는 유니크 인덱스(`users_email_partial_key`, `WHERE is_sso_user = false`)가 걸려 있어서 동일 이메일로의 계정 통합/변경 시도는 충돌(unique violation)로 거부된다 — Supabase는 계정 "병합" 기능을 자체 제공하지 않는다.

따라서 동일인이 서로 다른 이메일(예: gmail, naver)로 구글/네이버 등 다른 provider에 각각 가입하면 완전히 별개의 `user_id`로 남고, Supabase/OAuth 레벨에서는 이를 절대 감지할 수 없다.

국내 이커머스에서는 신규가입 쿠폰 등 혜택을 다중 계정으로 중복 수령하는 어뷰징이 흔한 문제다.
- 한국은 보통 PASS/NICE/KMC 휴대폰 본인인증(CI/DI)으로 막는다.
- 해외는 국가 단일 식별자가 없어, 전화번호 SMS 인증, 결제수단(카드/PayPal) 핑거프린팅, 디바이스 핑거프린팅, 배송지 매칭, Stripe Radar/Sift/Forter 같은 리스크 스코어링 플랫폼 등 여러 약한 신호를 조합해 확률적으로 방어한다 (완벽 차단이 아니라 어뷰징 비용을 높이는 전략).

## 결정

**(현재 미구현, 추후 작업 예정)**

Supabase `auth` 스키마(GoTrue 관리 영역)와 별개로, `public` 스키마에 자체 dedup 테이블을 만들어 백엔드(Spring Boot)에서 관리한다.

예시 스키마:

```sql
create table public.user_dedup_keys (
  id           bigint generated always as identity primary key,
  user_id      uuid not null references auth.users(id),
  key_type     text not null check (key_type in ('phone', 'card_fingerprint', 'device_fp')),
  key_hash     text not null,            -- 원본 PII는 저장하지 않고 sha256+salt 해시만 저장
  created_at   timestamptz not null default now(),
  unique (key_type, key_hash)            -- 이 조합이 이미 있으면 혜택 지급 불가로 판단
);
```

흐름:
1. 가입 또는 "혜택 받기" 시점에 휴대폰 SMS 인증(또는 결제수단 등록) 수행
2. 받은 식별값(휴대폰 번호, 카드 토큰 등)을 해시
3. `(key_type, key_hash)` 조회 → 이미 존재하면 다른 `user_id`가 그 키로 혜택을 받은 적 있다는 뜻 → 신규 혜택 차단 (가입 자체는 막지 않고 혜택만 스킵)
4. 없으면 insert 후 혜택 지급

DB 레벨 unique 제약으로 동시성 상황(레이스 컨디션)에서도 dedup을 보장하는 것이 핵심 — 애플리케이션 레벨 체크만으로는 동시 요청 시 뚫릴 수 있다.

## 대안

- **CI/DI 본인인증(국내 전용)**: PASS/NICE/KMC. 가장 강력하지만 해외 사용자에는 적용 불가, 인증 비용/연동 복잡도 있음.
- **전화번호 SMS 인증만**: 구현 간단하지만 VOIP/임시번호로 우회 가능. 1차 방어선으로만 적합.
- **결제수단 핑거프린팅**: 카드 BIN+마지막4자리+소유자명, PayPal account id 해시. 어뷰징은 결국 결제를 해야 혜택을 쓰므로 계정 단위보다 효과적이나, 결제 연동 전이라 지금 단계에서는 적용 불가.
- **디바이스 핑거프린팅(FingerprintJS 등)**: 시크릿모드/캐시삭제로 일부 우회되지만 비용을 올림. 외부 SaaS 의존 + 비용 발생.
- **외부 리스크 스코어링 플랫폼(Stripe Radar, Sift, Forter)**: 종합적이지만 비용이 크고 트래픽 규모상 현재 단계에는 과함.
- **아무것도 하지 않음 (현재 상태 유지)**: 지금처럼 신규가입 혜택 자체가 없거나 소규모라면 당장 리스크가 낮음. 혜택 기능을 실제로 출시하기 전까지는 보류 가능.

## 결과

- 신규가입 혜택/쿠폰 같은 기능을 출시하기 **전에** 반드시 먼저 구현해야 함 — 혜택 기능과 dedup 레이어는 묶어서 출시.
- 계정 "병합" 기능이 없는 상태이므로, 동일인이 이미 여러 계정을 만든 경우 CS 대응(수동 마이그레이션: `user_id` 일괄 업데이트 + 계정 삭제)이 별도로 필요함. 이 부분도 추후 어드민 기능으로 검토.
- 우선순위: 낮음 (혜택/쿠폰 기능이 실제로 기획되는 시점에 맞춰 재검토).

## 관련 세션
- [[sessions/2026-06-19]]
- [[ADR/ADR-002-social-login-via-supabase-custom-oidc-provider]] — 네이버 Custom OAuth가 `auth.users.email`을 항상 채우지 못할 수 있다는 사실이 이번 논의의 출발점이 됨
