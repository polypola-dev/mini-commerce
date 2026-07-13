# `catalog` 모듈

**상태**: Gradle 모듈 분리 완료(Phase 4, 2026-07-02). 내부 구조는 여전히 ⚠️ 헥사고날
미전환(레거시 플랫) — 모듈 경계만 그어졌을 뿐 POJO 도메인화는 별도 계획.

## 현재 구조

```
catalog/                             (Gradle 모듈, com.minicommerce.catalog 패키지)
├── Product, ProductOption          도메인 = JPA @Entity (분리 없음)
├── ProductRepository, ProductOptionRepository   Spring Data JPA, 엔티티 직접 참조
├── ProductController, ProductAdminController    엔티티/Repository를 서비스 계층 없이 직접 사용
├── ProductReader (+ ProductReaderImpl)   외부 노출용 공개 API — findProduct/findOption
├── InventoryClient                 order-api 재고 REST 어댑터(ADR-005 S3-3b)
└── CreateProductRequest / UpdateProductRequest / ProductOptionRequest / ProductResponse / ProductOptionResponse
```

Controller가 Repository를 직접 호출하는 구조로, `application`/`adapter` 계층 분리가
여전히 없다(모듈 내부는 Phase 4 스코프 밖). 예외 처리도 `jakarta.persistence.EntityNotFoundException`을
컨트롤러에서 그대로 사용하는 지점이 있다.

## 모듈 의존

- `catalog → shared-core`만 컴파일 의존한다. 재고는 `InventoryClient`(RestClient)가 order-api의
  `/internal/inventory` REST API를 호출해 조회·설정하며(ADR-005 S3-3b), inventory 모듈에는
  컴파일 의존하지 않는다 — order-api가 별도 프로세스로 분리돼도 그대로 동작한다.
- 외부(order 등)는 `ProductReader`(findProduct/findOption)를 통해서만 접근한다. order의
  `CatalogProductAdapter`(shop-api)가 이 API의 소비자.

## 알려진 부채

- 도메인(`Product`/`ProductOption`)이 `@Entity`로 기술에 침투됨.
- 서비스 계층 부재로 유즈케이스 경계가 컨트롤러/리포지토리에 흩어져 있음.
- 기술 예외(`EntityNotFoundException`) 누수.

## ADR-004 관련 결정 — Phase 4 완료 내역

catalog는 라이브러리 모듈로 추출 완료. `order`는 catalog의 내부 Repository가 아니라
`ProductReader` 공개 API에만 의존하도록 정리됨. catalog 자체의 헥사고날 내부 리팩터링
(POJO 도메인화 등) 시점은 아직 미정 — order 멀티모듈 전환이 끝난 뒤 별도 계획으로 판단한다.
