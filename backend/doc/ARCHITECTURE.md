# 백엔드 아키텍처 가이드 (AI Agent 필독)

> 새 기능 개발 시 **헥사고날 도메인 보호 원칙**이 깨지지 않게 하는 강제 규칙이다.
> 코드를 작성하기 전에 이 규칙을 먼저 따른다. 위반은 버그가 아니라 차단 대상이다.

## 스타일

- **헥사고날(포트 & 어댑터) + 모듈러 모놀리스(Spring Modulith)**, 추후 MSA 분리를 전제.
- 컨텍스트(=바운디드 컨텍스트) 패키지 루트: `com.minicommerce.<context>`
  (`order`, `catalog`, `inventory`, `cart`, `review`, `notification`).

## 레이어 & 의존 방향

컨텍스트마다 동일 구조. 의존은 **항상 안쪽(domain)으로만** 흐른다.

```
adapter.in(web) → application → domain ← application ← adapter.out(persistence/외부연동)
```

- `domain/` — 순수 POJO. 비즈니스 규칙. **기술 의존 0.**
- `application/` — 유즈케이스 구현 + 포트 인터페이스(`port/in`, `port/out`). 오케스트레이션.
- `adapter/in/web/` — 컨트롤러 / DTO (Driving Adapter).
- `adapter/out/**` — 영속성·외부연동 구현체 (Driven Adapter).

## 절대 규칙 (도메인 보호)

1. **`domain` 패키지에 기술 애너테이션 금지** — `@Entity`, `@Table`,
   `jakarta.persistence.*`, `org.springframework.*` 사용 안 함. 도메인은
   Lombok / commons-lang 수준의 의존만 허용한다.
2. **영속성은 별도 엔티티 + 매퍼로** — DB 저장은 `adapter/out/persistence/*JpaEntity`와
   도메인↔엔티티 Mapper를 거친다. 도메인 객체를 그대로 `JpaRepository`에 넘기지 않는다.
3. **application은 포트에만 의존** — 어댑터 구현체를 직접 import 금지. 예외도
   도메인 예외(예: `OrderNotFoundException`)를 쓰고, `jakarta.persistence.EntityNotFoundException`
   같은 기술 예외를 throw하지 않는다.
4. **컨트롤러는 유즈케이스 인터페이스(`port/in`)에만 의존** — 서비스 구현 클래스 직접 참조 금지.
5. **단방향 의존** — adapter는 application/domain을 알아도, domain은 adapter를 모른다.

## 크로스 컨텍스트 (모듈 간) 규칙

- 다른 컨텍스트 호출은 **포트 + 어댑터**로만. 예: `order`는 `ProductQueryPort` /
  `InventoryPort`를 정의하고, `adapter/out`에서 `catalog` / `inventory`의 **공개 API**에 연결한다.
- **다른 컨텍스트의 Repository·Entity·테이블에 직접 접근하거나 조인하지 않는다.**
- 비동기 통보는 **도메인 이벤트**로. 발행은 `ApplicationEventPublisher`(Modulith 아웃박스 경유).
  이벤트 타입은 **발행하는 컨텍스트가 소유**한다.

## MSA 전환 대비 규율 (지금부터 지킬 것)

- **모듈은 자기 테이블만 소유.** 크로스 컨텍스트 FK·조인 금지 → 추후 도메인별 DB 분리 가능하게.
- **단일 트랜잭션으로 두 컨텍스트를 동시에 commit하지 않는다.** 크로스 컨텍스트 상태 변경은
  동기 호출 + 보상(saga) 형태로. (예: inventory `reserve` → 실패 시 `release`, 성공 후 `confirm`.)
- 이벤트를 모듈 간 통합 계약으로 사용 → 추후 브로커(Kafka) 외부화 시 발행자 코드 무변경.
- 별도 부팅 모듈(admin·batch)을 **별도 서버로 띄우는 순간** in-process 이벤트는 불가 →
  브로커가 필요한 시점. 단순 스케줄 잡이면 `@Scheduled`로 같은 앱 안에서 처리(트리거 없으면 분리 금지).

## 현재 상태 / 알려진 부채

- 빌드: **단일 Gradle 모듈**, 패키지 단위 헥사고날. (목표: 멀티모듈 —
  `order-domain` / `order-infra` / `shop-api` / `order-admin` / `order-batch` 분리.)
- ⚠️ `order.domain.Order` / `OrderLine`이 아직 `@Entity` (규칙 1·2 위반, 리팩터링 예정).
  **새 도메인 객체는 이 패턴을 복제하지 말 것.**
- 깨끗한 참조 지점: `order/application/port/*`, `adapter/out` 분리 구조.

## 새 기능 추가 체크리스트

- [ ] 도메인 객체에 기술 애너테이션 없음 (순수 POJO)
- [ ] 유즈케이스는 `port/in`, 외부연동은 `port/out` 인터페이스를 경유
- [ ] 영속성은 `JpaEntity` + Mapper로 분리
- [ ] 타 컨텍스트는 포트로만 접근, 테이블 직접 접근·조인 없음
- [ ] 크로스 컨텍스트 상태 변경은 이벤트 또는 saga(보상) 형태
