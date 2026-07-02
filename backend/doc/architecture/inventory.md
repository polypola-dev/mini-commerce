# `inventory` 모듈

**상태**: Gradle 모듈 분리 완료(Phase 4, 2026-07-02). 내부 구조는 여전히 ⚠️ 헥사고날
미전환(레거시 플랫) — 모듈 경계만 그어졌을 뿐 POJO 도메인화는 별도 계획.

**배포 전략(ADR-005, 서비스그룹 (b))**: inventory는 **별도 Gradle 모듈로 유지**(order↔inventory
경계는 ArchUnit으로 강제)하되 **order-service에 함께 배포**된다. order와 같은 프로세스·같은 DB·같은
트랜잭션에 있으므로 `reserve/confirm/release`가 로컬 트랜잭션으로 원자적이다 → 분산 사가 불필요.
모듈 ≠ 배포단위라, 후속 (c) 완전분리(별도 서비스+DB) 추출 시 이 모듈만 떼면 된다.

## 현재 구조

```
inventory/                          (Gradle 모듈, com.minicommerce.inventory 패키지)
├── InventoryItem, InventoryReservation, InventoryHold, ReservationLine, ReservationStatus
│                                    도메인 = JPA @Entity (분리 없음)
├── InventoryReservationRepository  Spring Data JPA, 엔티티 직접 참조 (모듈 내부 전용)
├── InventoryService                공개 API. 유즈케이스 + 영속성 접근이 한 클래스에 혼재하지만
│                                    외부(order)에는 이 클래스만 노출
├── OutOfStockException             RuntimeException 직접 상속 (BusinessException 미사용)
└── ExpiredReservationReleaser      만료 예약 해제 (스케줄 잡)
```

`reserve`(예약) → 실패 시 release, 결제 성공 후 confirm 하는 saga형 흐름을 `InventoryService`가
직접 구현하고 있다. order의 `InventoryPort`/`InventoryAdapter`(shop-api)가 이 서비스를
in-process로 호출한다. `InventoryApiExceptionHandler`(`OutOfStockException` 전용
`@RestControllerAdvice`)는 shared-web과의 결합을 피하려고 inventory가 아니라 shop-api에
있다(패키지는 `com.minicommerce.inventory`로 동일, [architecture/shared.md](shared.md) 참조).

## 모듈 의존 및 공개 API

- `inventory → shared-core`만 의존. 다른 컨텍스트에 의존하지 않는다.
- 외부(order, catalog)는 `InventoryService`를 통해서만 접근한다. Phase 4에서
  `createReservationForOrder`/`confirmByOrderId`를 `InventoryService`에 추가해,
  기존에 order의 `InventoryAdapter`가 직접 하던 `InventoryReservationRepository` 접근을
  캡슐화했다 — `InventoryReservationRepository`는 이제 inventory 모듈 내부(자기 자신과
  `ExpiredReservationReleaser`)에서만 쓰인다.

## 알려진 부채

- 도메인(`InventoryItem` 등)이 `@Entity`로 기술에 침투됨.
- `InventoryService`가 포트 인터페이스 없이 구체 클래스로 직접 참조됨(order 쪽에서는
  `InventoryPort`로 감싸 호출하므로 order 자체는 안전하나, inventory 내부는 계층 분리 없음).
- `OutOfStockException`이 `global.BusinessException` 체계에 편입되지 않음.

## 하이브리드 재고 사가 계약 (ADR-005)

Redis 선예약(동시성/오버셀 방지)과 사가(예약-확정-복원 조율)는 **직교**한다 — Redis는 예약 저장소일
뿐 사가를 대체하지 않는다. order-service와 한 프로세스라 사가는 로컬 트랜잭션으로 얇게 유지된다.

| 단계 | 트리거 | 메커니즘 |
|---|---|---|
| **reserve** | 주문 생성 시 | 동기 로컬 호출 — Redis Lua(재고 체크 + `DECRBY` + 예약 해시 TTL 10분) + Postgres 예약 원장 |
| **confirm** | 결제 성공(`OrderPaid`) | 재고 확정 — 로컬 트랜잭션 |
| **release(신호)** | 결제 실패(`PaymentFailed`) | **즉시** 복원 (Redis `INCRBY`) |
| **release(미신호)** | 이탈/타임아웃/크래시 | `ExpiredReservationReleaser` 리퍼(`@Scheduled` 60s) — **백스톱** |

**주의**: 예약 해시의 TTL은 만료돼도 **차감된 재고(`stock:*`)를 자동 복구하지 않는다.** 재고 반환은
반드시 리퍼(또는 명시적 release)가 수행한다 — 그래서 Redis만으로 부족하고 Postgres 예약 원장이 필요하다.
리퍼는 현재 재고만 풀고 주문 상태는 방치하는 갭이 있어, S3에서 주문 `EXPIRED` 전이까지 연결한다.

**결제 후 취소(별도 에픽)**: PAID→CANCELED는 이미 confirm된 재고라 단순 release가 아니라 재입고
경로가 필요하다. 현 Redis `confirm` 스크립트가 `CONFIRMED→release`를 막고 있어(`status ~= 'RESERVED'`)
신규 재입고 스크립트가 필요하다. PG 환불 연동과 함께 후속 에픽으로 분리.

## Phase 4 완료 내역 (모듈 분리)

inventory는 라이브러리 모듈로 추출 완료, `InventoryService`가 공개 API로 정리됨.
`ExpiredReservationReleaser`(리퍼)는 order-service와 같은 프로세스에서 돌며, S4에서 order-batch로
이관할지 재검토한다(현재는 order-service 잔류가 기본).
