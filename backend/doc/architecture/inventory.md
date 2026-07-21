# `inventory` 서비스

**상태**: **완전분리 완료(GH #3, 전략 c, 2026-07-20, ADR-019)** — 독립 서비스 inventory-api +
전용 inventorydb. 내부 구조(inventory-core)는 여전히 ⚠️ 헥사고날 미전환(레거시 플랫)이나, 새로
추가한 표면(REST 컨트롤러/Kafka 컨슈머/유즈케이스)은 절대 규칙을 따른다. POJO 도메인화는 D9로 별도.

**배포 전략(ADR-019 — ADR-005 서비스그룹 (b)를 전략 (c)로 대체)**: inventory는 **독립 서비스+DB**로
분리됐다. order↔inventory는 더 이상 같은 프로세스/트랜잭션이 아니며 **하이브리드 분산 사가**로 조율한다:
- **reserve/release**: 동기 REST(order-api → inventory-api). 예약 ID = orderId(멱등 키).
- **confirm/restock**: 코레오그래피 — inventory-api가 `order.paid`/`order.canceled` Kafka 구독.
- **만료**: inventory-api 리퍼가 `inventory.reservation.expired` 발행 → order-batch가 EXPIRED 전이.

## 모듈 구성

```
inventory/
├── inventory-events/   이벤트 계약(의존성 0) — InventoryReservationExpiredEvent
├── inventory-core/     레거시 플랫 도메인·영속성 라이브러리(BOOT 아님)
│   ├── InventoryItem, InventoryReservation(@Entity), InventoryHold, ReservationLine, ReservationStatus
│   ├── InventoryReservationRepository   Spring Data JPA (inventory-api 내부 전용)
│   ├── InventoryService                 reserve/release/confirm/restock/setStock/availableStock
│   │                                    + Redis Lua(reserve/release/confirm/restock/forceConfirm)
│   ├── OutOfStockException, ReservationConflictException
│   └── ExpiredReservationReleaser       만료 리퍼(@Scheduled 60s, @SchedulerLock) → 만료 이벤트 발행
└── inventory-api/      BOOT 모듈(:8084) — inventorydb Flyway 소유
    ├── adapter/in/web   ReservationController(예약 사가 REST), InventoryInternalController(재고 조회/설정),
    │                    InventoryApiExceptionHandler(ProblemDetail 계약)
    ├── adapter/in/event OrderEventKafkaConsumer(order.paid→confirm, order.canceled→restock)
    ├── adapter/out/event InventoryEventExternalizationConfig(inventory.reservation.expired 라우팅)
    ├── application     StockService / ReservationService(port/in 유즈케이스, InventoryService 위임)
    └── config          SchedulerLockConfig(Redis LockProvider)
```

order-*는 inventory-core에 **컴파일 의존하지 않는다**(REST/Kafka 계약으로만 접근) — 이 경계를
Gradle 모듈 그래프가 물리적으로 강제한다. order-infra의 `InventoryRestAdapter`가 `InventoryPort`를
구현하고, 예약 사가 REST(409 out-of-stock/reservation-conflict, 503 unavailable)를 호출한다.

## reserve 순서 계약 (ADR-019 §3)

`InventoryService.reserveForOrder(orderId, items)`는 **① DB 원장 insert 커밋 → ② Redis Lua** 순서를
전제하며 트랜잭션으로 감싸지 않는다(감싸면 커밋이 Lua 뒤로 밀려 "Redis 차감했으나 원장 없음"이 되고
리퍼가 못 찾는다). 예약 해시 TTL은 만료(10분)와 분리해 86400초 — 만료 권위는 DB `expires_at` 단독,
"해시 없음 = 미차감" 불변식으로 release Lua가 크래시 창(return 3)을 안전하게 정리한다.

## 경합 정책 (ADR-019 §4)

- PG confirm 실패 시 release 안 함(PENDING_PAYMENT + TTL 재결제), confirm 사전 상태 가드로 창 축소.
- confirm 경합은 **payment-wins**: order.paid 컨슈머가 RELEASED/EXPIRED를 만나면 forceConfirm Lua로
  재차감+CONFIRMED 강제(순간 음수 재고 허용 + WARN).
- 재전달/순서역전은 원장 상태 가드 + Lua 멱등(restock RESTOCKED=2)으로 수렴.

## 알려진 부채

- inventory-core 내부는 레거시 플랫: 도메인(`InventoryItem` 등)이 `@Entity`로 기술 침투,
  `InventoryService`가 포트 인터페이스 없이 유즈케이스+영속성 혼재(D9로 별도 추적). inventory-api의
  새 표면(컨트롤러/컨슈머/유즈케이스)만 헥사고날 절대 규칙 준수.
- `OutOfStockException`/`ReservationConflictException`은 `BusinessException` 체계 밖 — 기존
  ProblemDetail(409, type) 응답 계약 보존을 위해 전용 `@RestControllerAdvice`로 매핑한다(양쪽:
  inventory-api `InventoryApiExceptionHandler`, order-api `InventorySagaExceptionHandler`).
- inventory-api 미발행 이벤트 재시도는 `republish-outstanding-events-on-restart`로 갈음 —
  레플리카 2+ 스케일 시 order-batch식 스위퍼 재검토(ADR-019 부채).
- 운영 Supabase에서 orderdb/inventorydb가 같은 public 스키마의 기본 `flyway_schema_history`를
  공유하는 문제는 OKE 이전 시점 과제(order-api와 공통).

## `availableStock()` write-on-read 제거 (GH #6, 해결됨)

과거 `availableStock(productId, defaultStock)`은 Redis에 키가 없으면 `defaultStock`을
그대로 **써버리는**(write-on-read) 구조였다 — 신규 상품이 재고 seed 전에 조회당하면(catalog의
배치조회 `/internal/inventory/stocks`는 미존재 시 default=0을 넘김) 재고가 0으로 영구 고착됐다.
`ProductAdminController.create()`에서 생성 시점에 `setStock`을 먼저 호출해 우회했었지만,
"조회가 부작용을 낸다"는 설계 자체는 남아 있었다.

현재는 `availableStock()`이 **순수 read-only**로 바뀌었다 — 키가 없으면 `defaultStock`을
반환만 하고 Redis에는 쓰지 않는다. 재고 초기화 책임은 `setStock` 호출부(`ProductAdminController.create()`
/`update()`)에만 있다. 로컬 개발용 상품 시드(GH #14, `db/seed/V1__seed_products.sql`)는
`products` 테이블에만 직접 INSERT하고 Redis는 건드리지 않는다 — 조회 시 항상 DB의
`Product.stock`을 `defaultStock`으로 넘기므로 Redis에 키가 없어도 값이 어긋나지 않는다.
"미초기화 상태"와 "재고 0"을 Redis 레벨에서 별도 구분(null/미정 상태 도입)하지는 않기로
결정했다 — DB의 `Product.stock`이 이미 선언적 소스이고, 호출부가 항상 그 값을 `defaultStock`으로
넘기므로 조회 시점에 모호함이 생기지 않는다(과도한 설계로 판단).

## 하이브리드 분산 사가 계약 (ADR-019, GH #3)

Redis 선예약(동시성/오버셀 방지)과 사가(예약-확정-복원 조율)는 **직교**한다 — Redis는 예약 저장소일
뿐 사가를 대체하지 않는다. inventory 완전분리로 order와 **다른 프로세스/DB**가 됐으므로 사가는 동기
REST + 코레오그래피 조합으로 조율된다(예약당 라인이 적어 원장/Redis 왕복은 얇다).

| 단계 | 트리거 | 메커니즘 |
|---|---|---|
| **reserve** | 주문 생성 시 | 동기 REST(order-api→inventory-api). ① DB 원장 insert 커밋 → ② Redis Lua(재고 체크+`DECRBY`+예약 해시 TTL 86400). 예약 ID=orderId 멱등 |
| **release** | 주문 생성 실패 보상 | 동기 REST(DELETE). release Lua return 3(해시 없음)/4(이미 RELEASED)로 크래시 창·재시도 정리 |
| **confirm** | 결제 성공(`order.paid`) | 코레오그래피 — inventory-api가 Kafka 구독. RESERVED→CONFIRMED, 경합 시 payment-wins force-confirm |
| **restock** | 주문 취소(`order.canceled`) | 코레오그래피 — CONFIRMED→RESTOCKED + Redis `INCRBY`(RESTOCKED=2 멱등) |
| **release(미신호)** | 이탈/타임아웃/크래시 | inventory-api의 `ExpiredReservationReleaser` 리퍼(`@Scheduled` 60s, `app.batch.enabled`) — **백스톱** |
| **EXPIRED 전이** | 리퍼 해제 성공 | `inventory.reservation.expired`(Modulith 아웃박스) → order-batch 구독 → `Order.markExpired()`(PENDING_PAYMENT 가드) |

**주의**: 예약 해시 TTL(86400s)이 소멸해도 차감된 재고(`stock:*`)는 자동 복구되지 않는다 — 재고 반환은
리퍼/release/restock이 수행한다(그래서 Postgres 예약 원장이 진실의 원천). 만료 권위는 DB `expires_at`
단독이며 해시 TTL은 "Redis 차감 여부의 증거"로만 쓴다(위 reserve 순서 계약).

**restockByOrderId(GH #4 승계)**: DB 원장을 진실의 원천으로 조회해 `restock()` 전이(CONFIRMED 가드,
이미 RESTOCKED면 no-op) 후 restock Lua 실행 — 해시 살아있으면 CONFIRMED 검증 후 INCRBY+RESTOCKED,
해시 소멸 시 DB 가드 신뢰 INCRBY+재기록. 이제 호출자는 order 트랜잭션이 아니라 inventory-api의
`order.canceled` 컨슈머다(S4 코레오그래피). `ReservationStatus.RESTOCKED`는 inventorydb Flyway V1에
반영(orderdb의 옛 테이블은 V4로 DROP).

## 완료 내역

- **Phase 4(2026-07-02)**: 라이브러리 모듈 분리, `InventoryService` 공개 API 정리.
- **GH #3 전략 c(2026-07-20, ADR-019)**: 독립 서비스+DB 완전분리, 하이브리드 분산 사가 전환.
  리퍼는 order-batch→inventory-api로 이관, 만료 이벤트는 in-process→Kafka 외부화.
