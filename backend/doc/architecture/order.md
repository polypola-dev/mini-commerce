# `order` 컨텍스트

**상태**: 헥사고날 도메인 순수화 완료(Phase 0–1). 멀티모듈 분리(Phase 2–7)는 진행 중 —
체크리스트/현황은 [GitHub Issue #1](https://github.com/polypola-dev/mini-commerce/issues/1)
"order 헥사고날 멀티모듈 전환 (Phase 0~7)" 참조.
결정 배경은 Obsidian ADR-004("order 서비스 헥사고날 + 멀티모듈 전환 및 MSA 대비").

## 패키지 구조

```
order/
├── domain/                 순수 POJO. 기술 의존 0.
│   ├── Order, OrderLine, OrderLineDraft, OrderStatus
│   └── exception/          OrderNotFoundException, OrderErrorCode
├── application/             유즈케이스 구현 + 포트
│   ├── OrderService, PlaceOrderCommand
│   ├── port/in/             PlaceOrderUseCase, CompletePaymentUseCase, GetOrdersUseCase
│   └── port/out/            OrderRepository, InventoryPort, ProductQueryPort, OrderEventPublisher
├── adapter/in/web/          OrderController, OrderAdminController, DTO
├── adapter/out/persistence/ OrderJpaEntity, OrderLineJpaEntity, OrderPersistenceMapper, OrderPersistenceAdapter
├── adapter/out/catalog/     CatalogProductAdapter (ProductQueryPort 구현, catalog 접근)
├── adapter/out/inventory/   InventoryAdapter (InventoryPort 구현, inventory 접근)
├── adapter/out/event/       SpringOrderEventAdapter
└── OrderPlacedEvent, OrderPaidEvent  도메인 이벤트 (order가 소유)
```

## 완료된 사항 (Phase 0–1)

- 안전망: `OrderPersistenceAdapterTest`(@DataJpaTest) — 저장/조회 라운드트립, 고객별 조회,
  상태변경 후 재저장 라인보존(merge/orphan 가드).
- `Order`/`OrderLine`을 순수 POJO화, `reconstitute()` 팩토리로 영속성 복원.
- `OrderJpaEntity`/`OrderLineJpaEntity` + `OrderPersistenceMapper` 신설, 도메인↔엔티티 매핑 분리.
- application 계층의 기술 예외(`jakarta.persistence.EntityNotFoundException`) 제거,
  도메인 예외(`OrderNotFoundException`, `BusinessException` 기반)로 교체.
- Modularity.verify() 통과 — global↔order 순환은 공통 베이스 예외로 해소, order→global 단방향 유지.

## 남은 계획 (Phase 2–7, 멀티모듈 전환)

`shared-core`/`shared-web` 분리, `catalog`/`inventory` 라이브러리화, `order-domain`/`order-infra`
분리, `shop-api`/`order-admin`/`order-batch` 부팅 모듈 분리, ArchUnit 경계 강제. 상세 Phase별
작업 내용과 진행 체크박스는 GitHub Issue를 단일 소스로 유지한다(이 문서에 복제하지 않음).

## 크로스 컨텍스트 접근

- `catalog`: `ProductQueryPort` → `CatalogProductAdapter`를 통해서만. `catalog`의 Repository를
  직접 참조하지 않는다.
- `inventory`: `InventoryPort` → `InventoryAdapter`를 통해서만. reserve 실패 시 release,
  결제 성공 후 confirm하는 saga 패턴을 따른다.
- `notification` 등에는 `OrderPlacedEvent`/`OrderPaidEvent` 도메인 이벤트로 알린다(Modulith
  이벤트 아웃박스 경유).

## 알려진 갭

- `BusinessException`/`ErrorCode` 기반 에러 처리는 **order에만** 도입됨. 다른 컨텍스트로의
  확산 여부는 미결정([architecture/shared.md](shared.md) 참조).
