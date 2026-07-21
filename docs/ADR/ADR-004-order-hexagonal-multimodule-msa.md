# ADR-004: order 서비스 헥사고날 + 멀티모듈(모듈러 모놀리스) 전환 및 MSA 대비

- 상태: 승인 (2026-06-30)

> ⚠️ **부분 supersede (2026-07-02, ADR-005)**: 아래 "단일 DB 유지"와
> "order 인접부만 모듈러 모놀리스에 머무름" 결정은 ADR-005의 MSA 서비스그룹(b) 전략으로 대체됨.
> 단 **도메인 순수화·모듈 경계 컴파일타임 강제·"모듈은 자기 테이블만 소유" 규율은 그대로 유효**하다.

## 컨텍스트

order 서비스에 헥사고날 아키텍처를 도입했으나 도메인 보호 원칙이 깨져 있었다.

- **도메인 침투**: `order.domain.Order` / `OrderLine`이 `@Entity` 등 JPA 기술 의존을 직접 가짐.
  `OrderPersistenceAdapter.save()`가 도메인 객체를 그대로 `JpaRepository`에 넘겨, 영속성
  포트/어댑터 분리가 사실상 무효화됨(매핑 부재).
- **기술 예외 누수**: `OrderService` 등 application 계층이 `jakarta.persistence.EntityNotFoundException`을 throw.
- **확장성 부재**: 단일 Gradle 모듈에 패키지로만 헥사고날을 구현. order-admin / order-batch처럼
  도메인·인프라를 공유하는 별도 실행 단위로 확장할 구조가 없음.
- order의 out 어댑터가 catalog / inventory의 Repository·Service 내부를 in-process로 직접 호출 →
  order만 독립적으로 떼어낼 수 없는 결합 존재.

추후 MSA 분리를 전제로 Spring Modulith(이벤트 아웃박스 + 단일 DB)를 택한 상태였다.

## 결정

1. **도메인 순수성 복구(Step A, 모듈 분리와 무관하게 선행)**
   - `Order` / `OrderLine`을 순수 POJO로. JPA 매핑은 `adapter/out/persistence/*JpaEntity` + Mapper로 분리.
   - application 계층의 기술 예외를 도메인 예외(`OrderNotFoundException` 등)로 교체.
2. **멀티모듈(모듈러 모놀리스) 전환(Step B)** — 추출 범위는 **order 인접부만**:
   `shared-core`(순수) / `shared-web`, `catalog` · `inventory`만 라이브러리 모듈로 추출,
   `order-domain`(JPA·Spring 의존 0) / `order-infra` / `shop-api`(BOOT) / `order-admin`(BOOT) / `order-batch`(BOOT).
   cart / review / notification은 당분간 shop-api에 잔류.
3. **도메인 보호의 강제 수단은 리뷰가 아니라 구조·빌드**:
   컴파일타임(모듈 클래스패스에서 JPA 제외) → ArchUnit + CI 게이트 → CLAUDE.md 규약 + 레퍼런스.
   Skill은 검증 실행 편의용 보조로만.
4. **MSA 대비 규율을 지금부터 적용**: 모듈은 자기 테이블만 소유(크로스 조인·FK 금지),
   단일 트랜잭션으로 두 컨텍스트 동시 commit 금지, 크로스 컨텍스트 변경은 이벤트 또는 saga(보상)로.
   (현 `inventory.reserve → 실패 시 release, 성공 후 confirm`이 saga의 씨앗.)

## 대안

- **단일 모듈 + Spring Modulith 유지**: 유효한 패턴이나 컴파일타임 경계 강제가 없고,
  별도 배포 단위(admin/batch) 확장에 약함.
- **전체 컨텍스트 모듈화**: 가장 깨끗하나 order와 무관한 코드까지 대규모 이동, 과한 공수.
- **이벤트/HTTP로 order 완전 디커플(분산화)**: 단일 DB의 트랜잭션 편의를 포기, 현 시점 오버엔지니어링.
- **도메인 보호를 Skill 리뷰로 강제**: 실행을 매번 기억해야 하고 사후 검출 → 가장 약한 방어선이라 기각.

## 결과

- 도메인이 기술로부터 격리되어 비즈니스 규칙 테스트·변경 용이. 멀티모듈 시 JPA가 도메인
  클래스패스에 없어 `@Entity` 침투가 **컴파일 단계에서 불가능**해짐.
- order-admin / order-batch를 별도 부팅 모듈(=별도 서버)로 확장 가능. 단 **별도 서버로 띄우는
  순간 in-process 이벤트 불가 → 브로커(Kafka)가 필요한 시점**. 단순 스케줄 잡이면 `@Scheduled`로
  같은 앱에서 처리(트리거 없으면 분리 금지).
- DB는 현재 단일 공유, MSA 전환 시 도메인별 DB로 분리. "모듈이 자기 테이블만 소유" 규율이 그 전제.
- 트레이드오프: JpaEntity + Mapper 보일러플레이트 증가, 멀티모듈 빌드 설정 복잡도 증가.
- 산출물: `backend/doc/ARCHITECTURE.md`(AI Agent 강제 규칙) + `.claude/CLAUDE.md` 참조 추가.
- 미결: 이벤트 외부화(브로커) 도입 시점, ArchUnit 규칙 세트, 멀티모듈 빌드 전환 실행(Phase 2~7).

## 관련 세션
- [[sessions/2026-06-30]]
