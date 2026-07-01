# `inventory` 컨텍스트

**상태**: ⚠️ 헥사고날 미전환 (레거시 플랫 구조).

## 현재 구조

```
inventory/
├── InventoryItem, InventoryReservation, InventoryHold, ReservationLine, ReservationStatus
│                                    도메인 = JPA @Entity (분리 없음)
├── InventoryReservationRepository  Spring Data JPA, 엔티티 직접 참조
├── InventoryService                유즈케이스 + 영속성 접근이 한 클래스에 혼재 (포트 없음)
├── OutOfStockException             RuntimeException 직접 상속 (BusinessException 미사용)
└── ExpiredReservationReleaser      만료 예약 해제 (스케줄 잡)
```

`reserve`(예약) → 실패 시 release, 결제 성공 후 confirm 하는 saga형 흐름을 `InventoryService`가
직접 구현하고 있다. order의 `InventoryPort`/`InventoryAdapter`가 이 서비스를 in-process로
호출한다.

## 알려진 부채

- 도메인(`InventoryItem` 등)이 `@Entity`로 기술에 침투됨.
- `InventoryService`가 포트 인터페이스 없이 구체 클래스로 직접 참조됨(order 쪽에서는
  `InventoryPort`로 감싸 호출하므로 order 자체는 안전하나, inventory 내부는 계층 분리 없음).
- `OutOfStockException`이 `global.BusinessException` 체계에 편입되지 않음.

## ADR-004 관련 결정

catalog와 동일하게 Phase 4에서 **라이브러리 모듈로 추출**, `InventoryService`를 공개 API로
정리할 예정. 별도 부팅 모듈(admin/batch)이 생기면 예약 만료 처리(`ExpiredReservationReleaser`)가
어느 프로세스에서 도는지 재검토가 필요하다(Phase 6 크로스프로세스 이벤트 리스크와 연관).
