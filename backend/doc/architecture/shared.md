# `shared-core` / `shared-web` 모듈

**상태**: ADR-004 Phase 3 완료(2026-07-01). 과거 단일모듈의 `global` 패키지가
`shared-core`/`shared-web` 두 Gradle 모듈로 물리적 분리됨. 패키지명(`com.minicommerce.global`,
`com.minicommerce.global.security`)은 변경 없이 유지.

## 현재 구조

```
shared-core/global/              (spring/jpa 의존 0, 순수)
├── PageResult                        페이지네이션 응답 래퍼
├── BusinessException                 모든 도메인 예외의 베이스 (RuntimeException 상속)
└── ErrorCode, ErrorType               에러 코드 카탈로그 + HTTP 상태 매핑용 타입

shared-web/global/                (spring-web 의존 허용)
├── ApiExceptionHandler                @RestControllerAdvice — BusinessException/
│                                       EntityNotFoundException/MethodArgumentNotValidException
│                                       제네릭 핸들러만 담당
├── WebConfig                          웹 공통 설정 (CORS, 보안필터 등록)
└── security/
    ├── JwtVerificationFilter          JWT 인증 필터
    └── AdminAuthorizationFilter       관리자 권한 필터
```

## BusinessException 적용 현황

`BusinessException`/`ErrorCode` 기반 표준 에러 처리는 **현재 `order` 컨텍스트에만 적용**됨
(`OrderNotFoundException`이 이를 상속). `catalog`/`inventory`/`cart`/`review`/`notification`은
아직 `RuntimeException` 직접 상속(`OutOfStockException`, `CartFullException` 등) 또는
`jakarta.persistence.EntityNotFoundException` 누수 상태 — 다른 컨텍스트로의 확산 여부는
미결정.

## shared-web과 비즈니스 컨텍스트 간 결합 금지 원칙

Phase 3 작업 중 기존 `ApiExceptionHandler`가 `inventory.OutOfStockException`을 직접
`@ExceptionHandler`로 잡고 있던 것이 발견됨. `shared-web`은 `shared-core`에만 의존해야
하는데(shop-api가 shared-web에 의존하므로 역방향 의존은 순환이 되어 컴파일 자체가 불가능),
이 결합 때문에 그대로는 이동이 불가능했음. `OutOfStockException`을 `BusinessException`
체계로 편입시키는 대신(위 "적용 현황"의 미결정 사항을 이번에 확정하지 않음), 핸들러를
물리적으로 분리:
- `shared-web.ApiExceptionHandler` — 컨텍스트에 종속되지 않는 제네릭 예외만 처리
- `shop-api`에 컨텍스트 전용 어드바이스(예: `inventory.InventoryApiExceptionHandler`)를
  별도로 두고 해당 컨텍스트의 특화 예외를 처리

**앞으로 새 컨텍스트 전용 예외를 위한 `@ExceptionHandler`를 추가할 때는 shared-web이 아니라
해당 컨텍스트가 위치한 모듈(현재는 shop-api)에 별도 `@RestControllerAdvice`로 추가한다.**

## ADR-004 Phase 3 완료 내역

- `PageResult` + `BusinessException`/`ErrorCode`/`ErrorType` → **shared-core**로 분리 완료
  (순수, 스프링/JPA 의존 0). `order-domain`이 `PageResult`를 반환형으로 쓰기 때문에
  (`OrderRepository.findAllPaged`), 도메인 순수성 유지를 위해 Phase 5보다 선행됨.
- 보안 필터(`JwtVerificationFilter`, `AdminAuthorizationFilter`)·`WebConfig`·
  `ApiExceptionHandler`(제네릭 부분) → **shared-web**으로 분리 완료.
- import 경로는 패키지명 유지로 대부분 변경 불필요, `ApiExceptionHandler` 분리에 따른
  import만 조정.

상세 작업 순서/이력은 [GitHub Issue #1](https://github.com/polypola-dev/mini-commerce/issues/1)
(order 헥사고날 멀티모듈 전환 Phase 0~7)을 본다.
