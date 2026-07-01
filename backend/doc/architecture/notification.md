# `notification` 컨텍스트

**상태**: ⚠️ 헥사고날 미전환 (레거시 플랫 구조).

## 현재 구조

```
notification/
├── Notification, NotificationType, NotificationStatus   도메인 = JPA @Entity (분리 없음)
├── NotificationRepository            Spring Data JPA, 엔티티 직접 참조
├── NotificationService                유즈케이스 + 영속성 접근 혼재 (포트 없음)
├── NotificationController             서비스 구현 클래스를 직접 참조
├── NotificationSender / LogNotificationSender   발송 채널 추상화 (현재는 로그 발송만)
└── NotificationResponse
```

`order`의 `OrderPlacedEvent`/`OrderPaidEvent` 등 도메인 이벤트를 구독해 알림을 생성하는
역할(Spring Modulith 이벤트 아웃박스 경유). 이벤트 소비 자체는 컨텍스트 경계를 지키고 있으나,
notification 내부 구조는 다른 레거시 컨텍스트와 동일하게 계층 분리가 없다.

## 알려진 부채

- 도메인(`Notification`)이 `@Entity`로 기술에 침투됨.
- `NotificationController`가 `NotificationService` 구현 클래스를 직접 참조.

## ADR-004 관련 결정

order 인접부 추출 범위에 포함되지 않음 — **당분간 `shop-api`에 잔류**. order-admin/order-batch가
별도 프로세스로 분리되면, 그 프로세스에서 발행하는 이벤트를 notification(shop-api 프로세스)이
in-process로 못 받는 문제가 생긴다 — 이벤트 아웃박스 외부화/재발행 경로 점검이 필요
(Phase 6 크로스프로세스 이벤트 리스크, [GitHub Issue #1](https://github.com/polypola-dev/mini-commerce/issues/1) 참조).
