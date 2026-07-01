# `global` (공통) 패키지

**상태**: 컨텍스트가 아닌 공용 유틸/공통 관심사 모음. 향후 `shared-core`/`shared-web`으로
분리 예정(ADR-004 Phase 3).

## 현재 구조

```
global/
├── PageResult                        페이지네이션 응답 래퍼 (순수, 기술 의존 없음)
├── BusinessException                 모든 도메인 예외의 베이스. Spring 의존 없이 동작(RuntimeException 상속)
├── ErrorCode, ErrorType               에러 코드 카탈로그 + HTTP 상태 매핑용 타입
├── ApiExceptionHandler                @RestControllerAdvice, BusinessException → HTTP 응답 매핑
├── WebConfig                          웹 공통 설정
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

## ADR-004 관련 결정 (Phase 3)

- `PageResult` + `BusinessException`/`ErrorCode` → **shared-core**로 분리 (순수, 스프링/JPA 의존 0).
  `order-domain`이 `PageResult`를 반환형으로 쓰기 때문에(`OrderRepository.findAllPaged`),
  도메인 순수성을 유지하려면 이 분리가 선행되어야 한다.
- 보안 필터(`JwtVerificationFilter`, `AdminAuthorizationFilter`)·`WebConfig`·
  `ApiExceptionHandler` → **shared-web**으로 분리 (스프링 의존 허용).
- import 경로 일괄 갱신 필요.

상세 작업 순서는 [GitHub Issue #1](https://github.com/polypola-dev/mini-commerce/issues/1)
(order 헥사고날 멀티모듈 전환 Phase 0~7)의 Phase 3 항목을 본다.
