# `review` 컨텍스트

**상태**: ⚠️ 헥사고날 미전환 (레거시 플랫 구조).

## 현재 구조

```
review/
├── Review                          도메인 = JPA @Entity (분리 없음)
├── ReviewRepository                 Spring Data JPA, 엔티티 직접 참조
├── ReviewService                    유즈케이스 + 영속성 접근 혼재 (포트 없음)
├── ReviewController                 서비스 구현 클래스를 직접 참조
└── CreateReviewRequest / ReviewResponse / ReviewListResponse
```

## 알려진 부채

- 도메인(`Review`)이 `@Entity`로 기술에 침투됨.
- `ReviewController`가 `ReviewService` 구현 클래스를 직접 참조(유즈케이스 인터페이스 없음).
- 기술 예외(`EntityNotFoundException`) 누수 지점 존재.

## ADR-004 관련 결정

order 인접부 추출 범위에 포함되지 않음 — **당분간 `shop-api`에 잔류**. 헥사고날 내부
전환 시점은 미정.
