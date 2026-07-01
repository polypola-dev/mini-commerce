# `catalog` 컨텍스트

**상태**: ⚠️ 헥사고날 미전환 (레거시 플랫 구조).

## 현재 구조

```
catalog/
├── Product, ProductOption          도메인 = JPA @Entity (분리 없음)
├── ProductRepository, ProductOptionRepository   Spring Data JPA, 엔티티 직접 참조
├── ProductController, ProductAdminController    엔티티/Repository를 서비스 계층 없이 직접 사용
├── CreateProductRequest / UpdateProductRequest / ProductOptionRequest / ProductResponse / ProductOptionResponse
└── SeedData
```

Controller가 Repository를 직접 호출하는 구조로, `application`/`adapter` 계층 분리와
포트 인터페이스가 없다. 예외 처리도 `jakarta.persistence.EntityNotFoundException`을
컨트롤러에서 그대로 사용하는 지점이 있다.

## 알려진 부채

- 도메인(`Product`/`ProductOption`)이 `@Entity`로 기술에 침투됨.
- 서비스 계층 부재로 유즈케이스 경계가 컨트롤러/리포지토리에 흩어져 있음.
- 기술 예외(`EntityNotFoundException`) 누수.

## ADR-004 관련 결정

order 헥사고날 멀티모듈 전환(Phase 4)에서 catalog는 **라이브러리 모듈로 추출**될 예정.
이때 외부 노출용 `ProductReader`(findProduct/findOption) 같은 공개 API를 정의하고,
`order` 등 다른 컨텍스트는 catalog의 내부 Repository가 아니라 이 공개 API에만 의존하도록
정리한다. catalog 자체의 헥사고날 내부 리팩터링(POJO 도메인화 등) 시점은 아직 미정 —
order 멀티모듈 전환이 끝난 뒤 별도 계획으로 판단한다.
