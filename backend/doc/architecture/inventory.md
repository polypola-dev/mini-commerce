# `inventory` 모듈

**상태**: Gradle 모듈 분리 완료(Phase 4, 2026-07-02). 내부 구조는 여전히 ⚠️ 헥사고날
미전환(레거시 플랫) — 모듈 경계만 그어졌을 뿐 POJO 도메인화는 별도 계획.

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

## ADR-004 관련 결정 — Phase 4 완료 내역

inventory는 라이브러리 모듈로 추출 완료, `InventoryService`가 공개 API로 정리됨. 별도
부팅 모듈(admin/batch)이 생기면 예약 만료 처리(`ExpiredReservationReleaser`)가 어느
프로세스에서 도는지 재검토가 필요하다(Phase 6 크로스프로세스 이벤트 리스크와 연관).
