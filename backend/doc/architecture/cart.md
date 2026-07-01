# `cart` 컨텍스트

**상태**: ⚠️ 헥사고날 미전환 (레거시 플랫 구조).

## 현재 구조

```
cart/
├── Cart, CartItem                  도메인 = JPA @Entity (분리 없음)
├── CartRepository                  Spring Data JPA, 엔티티 직접 참조
├── CartService                     유즈케이스 + 영속성 접근 혼재 (포트 없음)
├── CartController                  서비스 구현 클래스를 직접 참조
├── CartFullException               RuntimeException 직접 상속 (BusinessException 미사용)
└── AddCartItemRequest / UpdateCartItemRequest / CartResponse / CartItemResponse
```

## 알려진 부채

- 도메인(`Cart`/`CartItem`)이 `@Entity`로 기술에 침투됨.
- `CartController`가 `CartService` 구현 클래스를 직접 참조(유즈케이스 인터페이스 없음).
- `CartFullException`이 `global.BusinessException` 체계에 편입되지 않음.

## ADR-004 관련 결정

order 인접부 추출 범위에 포함되지 않음 — **당분간 `shop-api`(order 등과 같은 부팅 모듈)에
잔류**한다. catalog/inventory 라이브러리화 이후 cart가 그 공개 API(`ProductReader` 등)로
의존을 갈아타야 할 수 있다(현재 cart가 catalog/inventory를 어떻게 참조하는지는 별도 확인 필요).
헥사고날 내부 전환 시점은 미정.
