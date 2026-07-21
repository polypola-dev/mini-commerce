# ADR-019: inventory 완전분리 — 별도 서비스+DB, 하이브리드 분산 사가 (GH #3, 전략 c)

- 상태: Accepted (2026-07-20)
- 관련: ADR-005(서비스그룹 b — 이 ADR이 전략 c로 대체), GH #3
- 선행 완료: order MSA 전환(#2), order:order-events 계약 모듈(#5/D2)

## 배경

ADR-005는 order+inventory를 **서비스그룹(b)**로 묶어 같은 프로세스·DB·트랜잭션에 두고
reserve/confirm/release를 로컬 트랜잭션으로 원자적으로 처리했다. 고응집 도메인을 쪼개면 매
주문이 분산 사가가 되어 과투자로 판단해 미뤘던 결정이다. 이 ADR은 학습 목적으로 **전략(c)**,
즉 inventory를 독립 서비스(inventory-api) + 전용 DB(inventorydb)로 추출하고 order↔inventory를
분산 사가로 전환한다.

## 결정

### 1. 모듈/배포 분리
- `inventory` 라이브러리 모듈을 `inventory:inventory-core`로 재배치하고, BOOT 모듈
  `inventory:inventory-api`(:8084)와 이벤트 계약 모듈 `inventory:inventory-events`(의존성 0,
  order:order-events 패턴)를 신설한다.
- order-api/order-admin/order-batch/order-infra는 **inventory-core 컴파일 의존을 버린다** —
  이 경계가 order가 inventory 내부에 손대지 못하게 하는 물리적 강제다(Gradle 모듈 그래프).
- 전용 **inventorydb**(로컬/CI는 postgres 인스턴스의 별도 DB, 운영은 Supabase public 스키마에
  Flyway로 통합 — orderdb와 동일 원칙). inventory-api가 Flyway 단독 소유.

### 2. 하이브리드 사가 (동기 REST + 코레오그래피)
- **reserve/release는 동기 REST**(order-api → inventory-api). 예약 ID = orderId(멱등 키) —
  order-api가 orderId를 선생성해 넘기고, 재시도는 기존 RESERVED 원장으로 수렴(이중 차감 없음).
  품절은 409 out-of-stock ProblemDetail로 즉시 피드백, 어댑터가 도메인 예외로 복원한다.
- **confirm/restock은 코레오그래피**(inventory-api가 기존 `order.paid`/`order.canceled` Kafka
  구독). order는 이벤트 발행만 하고 결제/취소 트랜잭션에서 원격 재고 호출이 사라진다.
- **만료는 inventory 발행**: 리퍼(ExpiredReservationReleaser)가 inventory-api 소유로 이관되고,
  해제 성공 시 신규 `inventory.reservation.expired`를 Modulith 아웃박스로 발행 → order-batch가
  구독해 주문을 EXPIRED로 전이(과거 in-process 이벤트를 외부화).

### 3. 예약 원장 = 진실의 원천, Redis = 예약 저장소
- reserve 순서 계약: **① DB 원장 insert 커밋 → ② Redis Lua(재고 차감)**. 트랜잭션으로 감싸지
  않는다 — 감싸면 커밋이 Lua 뒤로 밀려 "Redis 차감했으나 원장 없음"이 되고 리퍼가 영영 못 찾는다.
- 예약 해시 TTL을 예약 만료(10분)와 분리해 **86400초**로 둔다. 만료 권위는 DB `expires_at` 단독.
  이로써 "해시 없음 = Redis 차감 없었음" 불변식이 성립하고, release Lua가 return 3(해시 없음)으로
  DB커밋↔Lua 크래시 창을 INCRBY 없이 원장만 전이해 정리한다(과거 재고 누수 버그 동시 해소).

### 4. 경합 정책
- **PG confirm 실패 시 release 안 함**(현행 유지) — PENDING_PAYMENT로 TTL 내 재결제 허용, 만료는
  리퍼가 보상. `OrderService.confirm()` 시작부에 예약 상태 사전 가드(GET status)를 두어 만료 경합
  창을 PG 호출 직전으로 좁힌다.
- **confirm 경합은 payment-wins**: order.paid 컨슈머가 RELEASED/EXPIRED를 만나면 force-confirm
  Lua로 재차감+CONFIRMED 강제(순간 음수 재고 허용, WARN+메트릭). 역방향은 `markExpired()`의
  PENDING_PAYMENT 가드로 안전.
- 재전달/순서역전: 원장 상태 가드 + Lua 멱등(restock RESTOCKED=2 스킵)으로 수렴. restock이 아직
  CONFIRMED 아닌 예약(취소가 결제확정보다 먼저 소비된 드문 순서)은 IllegalState 전파 → Kafka 재시도.

## 배제한 대안
- **전 구간 동기 REST**: 결제 트랜잭션 안에 원격 호출 2개(PG+inventory)가 들어가고 이벤트 사가
  학습 효과가 없다. 사용자가 하이브리드를 선택.
- **inventory 내부 헥사고날 전환 동반**: 범위 폭발. inventory-core 내부는 레거시 플랫 유지, 새
  코드(REST/컨슈머/유즈케이스)만 절대 규칙 준수(D9로 별도 추적).
- **SealedSecrets류 데이터 이관**: 사이드 프로젝트라 로컬 예약 데이터는 폐기(FK 없음 확인).

## 부채 / 후속
- inventory-api 미발행 이벤트 재시도는 `republish-outstanding-events-on-restart`로 갈음한다 —
  레플리카 2+ 스케일 시 order-batch식 IncompleteEventSweeper 도입 재검토.
- 운영(Supabase) DB 분리 시점의 Flyway history 충돌(orderdb/inventorydb가 같은 public 스키마의
  기본 `flyway_schema_history` 공유)은 order-api와 공통 미해결 과제 — OKE 이전 때 스키마 분리 또는
  `flyway.table` 분리로 해소.
- inventory-api를 별도 서비스로 뺐으므로 서비스간 `/internal` 인증(GH #13 B3)의 대상이 실제로
  생겼다 — 필요 시 함께 설계.

## 검증
compose e2e 전 경로(예약/멱등/품절/release 보상/confirm/restock/만료→EXPIRED/payment-wins
force-confirm) + kind 실배포(db-init inventorydb 생성, Flyway 4테이블, Service 경유 예약,
NetworkPolicy 차단) 실측. 커밋 S1~S5.
