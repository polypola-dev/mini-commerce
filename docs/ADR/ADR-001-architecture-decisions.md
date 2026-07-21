# 📝 아키텍처 의사결정 기록 (ADR)
> **Architecture Decision Record (ADR)**
>
> * **일시**: 2026-06-01
> * **상태**: 승인됨 (Approved)
> * **참여자**: USER (설계 제안 및 최종 승인), Antigravity (AI 아키텍트)

---

## 1. 배경 및 컨텍스트 (Context)
본 프로젝트는 **Mini-Commerce MVP**를 구축한 후, 미래 비즈니스 확장 및 서비스 규모에 맞춰 **도메인 중심의 MSA(Microservices Architecture)로의 확장성**을 미리 확보하는 것을 핵심 목표로 합니다. 

인프라 구성 비용을 0원으로 수렴시키면서도, 정적 리소스(이미지)의 효율적 서빙과 보안 관점에서 해커의 공격(XSS, SQL Injection, 악성 파일 업로드, API 다이렉트 침투)을 원천 방어하기 위한 세부 보안/리소스 아키텍처를 결정하고자 합니다.

---

## 2. 의사결정 사항 (Decisions)

### 📌 결정 1: 도메인별 선택적 헥사고날 아키텍처(Selective Hexagonal Architecture) 도입
* **결정 내용**: 모든 도메인 모듈에 헥사고날을 강제하지 않고, 비즈니스 복잡도에 따라 하이브리드로 적용합니다.
  * **주문(Order) 도메인**: 외부 결제 API, 재고 캐시 등 인프라 기술과의 결합도가 높고 정책이 복잡하므로 **헥사고날 아키텍처(포트와 어댑터)**를 적용하여 비즈니스 코어를 완벽히 보존합니다.
  * **상품(Catalog) 및 인증(Identity) 도메인**: 단순 CRUD 위주의 입출력 성격이 강하므로 직관적인 **계층형(Layered) 아키텍처**로 패키지를 분리하여 신속하게 개발합니다.

---

### 📌 결정 2: Redis Lua Script 원자성 기반 동시성 제어
* **결정 내용**: 초고동시성 하의 재고 차감 로직의 무결성을 지키기 위해 **Redis Lua Script**의 단일 명령 원자성을 활용합니다.
* **결정 배경**: 재고를 Redis에 저장하는 현재 구조에서는 Lua Script가 단일 명령으로 "재고 확인 → 차감 → 예약 기록"을 원자적으로 처리합니다. Redisson 분산 락의 강점인 "JPA DB 트랜잭션 감싸기"는 재고가 DB에 저장될 때 적합하며, Redis 재고 구조에서는 Lua Script가 더 경량하고 직관적입니다.
* **트레이드오프**: 향후 재고를 DB(JPA)로 이전할 경우 Redisson MultiLock 방식으로 전환을 검토합니다.

---

### 📌 결정 3: BFF(Backend-For-Frontend) 패턴을 활용한 구글 인증 및 검증
* **결정 내용**: 프론트엔드 자바스크립트 영역에 토큰을 노출하지 않고, 프론트 서버(BFF)가 토큰을 보관하며 브라우저에는 **HttpOnly, SameSite, Secure 옵션의 쿠키**만 전달하여 XSS 위험을 원천 차단합니다.
* **결정 배경**: Next.js 서버(BFF)가 토큰 발급 및 관리를 안전하게 대행하고, 브라우저에는 자바스크립트로 접근할 수 없는 **HttpOnly, SameSite, Secure 옵션의 쿠키**만 전달하여 XSS 해킹 위험을 원천 차단합니다.

---

### 📌 결정 4: Spring Modulith의 Event Publication Registry를 통한 Outbox 패턴 구현 및 Kafka 전환 전략
* **결정 내용**:
  1. **초기 모놀리스**: 아웃박스 테이블 및 스케줄러를 직접 구현하는 오버헤드를 줄이기 위해, **Spring Modulith의 `Event Publication Registry`** 기술을 활용하여 Outbox 패턴을 자동화합니다.
  2. **MSA 확장 시점**: 코드 변경 없이 **Kafka 메시지 브로커**로 이벤트를 외재화하도록 점진적 전환 설계를 반영합니다.
* **결정 배경 및 정합성 보장**:
  * *Spring Modulith의 정답 제시*: Spring Modulith가 제공하는 `spring-modulith-starter-jpa` 의존성은 이벤트 발행 시점에 **`event_publication` 테이블**에 이벤트를 자동으로 영속화합니다. 리스너 처리가 성공적으로 끝나면 완료 마킹을 하며, 어플리케이션 재시작 시 미완료 이벤트를 자동으로 재발행해줍니다. (Zero Event Loss 확보)
  * *Kafka의 위치 및 모듈러 모놀리스에서의 역할*:
    * 초기 모듈러 모놀리스 단계에서는 로컬 PC 인프라 비용 및 운영 복잡성을 낮추기 위해 **실물 Kafka 설치를 배제**하고 스프링 Modulith의 내부 비동기 이벤트로 도메인 간 결합을 끊습니다.
    * 훗날 특정 도메인(예: Notification)을 별도 마이크로서비스로 추출할 때, Spring Modulith의 **Kafka Starter(`spring-modulith-events-kafka`)** 설정만 추가로 활성화합니다. 이렇게 하면 **기존 Java 코드(`ApplicationEventPublisher.publishEvent`)는 한 줄도 고치지 않고**, 발행되는 이벤트가 자동으로 Kafka 토픽으로 발행되어 신뢰도 높게 다른 서비스로 전달됩니다.

---

### 📌 결정 5: Next.js API Routes & Middleware를 활용한 실용적 API Gateway/BFF 구현
* **결정 내용**: 별도의 무거운 인프라(Spring Cloud Gateway, Kong 등)를 초기에 추가 도입하지 않고, **Next.js의 Middleware 및 API Routes**를 API Gateway 겸 BFF로 적극 활용합니다.
* **결정 배경 및 트레이드오프**:
  * `Next.js Gateway/BFF`: 프론트엔드 배포 환경에 자연스럽게 내장되어 있으며, Secure Cookie를 복호화해 스프링 백엔드로 `Bearer JWT`를 실어 보내는 **BFF 본연의 역할 수행에 가장 최적화**되어 있습니다. Next.js Edge Middleware에서 Rate Limit 및 간단한 라우팅을 초고속으로 수행할 수 있습니다.
  * *MSA 전환 시점의 확장*:
    * 훗날 마이크로서비스가 많아지면 Next.js BFF와 스프링 백엔드 사이에 경량화된 내부 API Gateway (Spring Cloud Gateway 또는 Kong)를 한 레이어 추가하는 방식으로 우아하게 마이그레이션합니다.

---

### 📌 결정 6: Spring Boot 3 표준 Micrometer Tracing 및 RFC 7807 예외 처리 도입
* **결정 내용**: 전통적인 수동 MDC 필터 구현 대신, 현대 클라우드 네이티브 표준 관측성(Observability) 기술인 **Micrometer Tracing (Brave/OpenTelemetry Bridge)**을 도입하여 비동기 환경에서도 완벽한 분산 트레이싱을 구현합니다.
* **구체적 설계 및 변경 배경**:
  1. **수동 MDC(ThreadLocal)의 한계와 비동기 전파 이슈**:
     * 수동 MDC 방식은 `ThreadLocal` 기반이므로, 우리가 채택한 **Spring Modulith의 비동기 리스너(`@ApplicationModuleListener` 또는 `@Async`) 환경에서 스레드가 전환될 때 Trace ID가 유실**되는 심각한 결함이 있습니다.
     * 컨텍스트 전파(Context Propagation) 코드를 일일이 수동 작성하는 대신 스프링 부트 3 표준인 **Micrometer Tracing**을 채택합니다.
  2. **W3C Trace Context 표준 준수 및 자동 로깅**:
     * Micrometer Tracing은 W3C 표준 규격인 `traceparent` 헤더(예: `00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01`)를 자동으로 파싱하고, 비동기 스레드 전환 시에도 Trace ID와 Span ID의 컨텍스트 전파를 완벽히 보장합니다.
     * 특별한 커스텀 필터 작성 없이 의존성 추가만으로 Logback 연동을 통해 로그 포맷에 자동으로 `[TraceID, SpanID]`를 출력해 줍니다.

---

### 📌 결정 7: Java 21 기반 가상 스레드(Virtual Threads) 도입 및 동시성 병목 방어 전략
* **결정 내용**: 처리 성능 극대화 및 스레드 블로킹으로 인한 성능 저하를 방어하기 위해 **Java 21의 가상 스레드(Virtual Threads)** 기능을 전면 활성화하고, 스레드 피닝/커넥션 풀 고사 대책을 적용합니다.
* **스프링 활성화 옵션**:
  * `spring.threads.virtual.enabled=true`
* **가상 스레드 도입 시 핵심 아키텍처 고려사항**:
  1. **Carrier Thread Pinning (스레드 피닝 현상) 방지**:
     * *대책*: 로깅(Logback) 라이브러리 및 데이터베이스 JDBC 드라이버(PostgreSQL)의 최신 릴리즈 버전을 적용하고, JVM 가동 매개변수에 `-Djdk.tracePinnedThreads=full` 설정을 켜서 모니터링 체계를 확보합니다. `synchronized`는 필요시 Java의 `ReentrantLock`으로 적극 교체합니다.
  2. **HikariCP 커넥션 풀 고사(Starvation) 방어**:
     * *대책*: 데이터베이스 I/O가 동반되는 영역에는 동시 처리 세마포어(`Semaphore`) 또는 속도 제한을 애플리케이션 레이어에서 선행 제어하여 DB 커넥션 과부하를 제어합니다.
  3. **ThreadLocal 메모리 제어**:
     * *대책*: Micrometer Tracing이 사용하는 Trace Context 정보 외에 무거운 DTO나 도메인 데이터를 `ThreadLocal`에 절대 적재하지 않습니다.

---

### 📌 결정 8: Vercel & Supabase & Upstash 기반의 하이브리드 서버리스 인프라 채택 및 평생 $0 운영 전략
* **결정 내용**: 초기 비용을 0원으로 낮추기 위해 **하이브리드 서버리스 모델**을 적용하고, UptimeRobot 핑 자동화를 연결해 365일 24시간 DB 상시 활성화를 유지합니다.
* **인프라 매핑 결정**:
  1. **Next.js 호스팅 & BFF**: **`Vercel (PaaS)`** Hobby 플랜 ($0/월)
  2. **Identity & OAuth**: **`Supabase Auth (BaaS)`** ($0/월) - 월간 활성 사용자 5만 명 무료 티어 획득. 스프링 백엔드는 단순히 Supabase JWT 서명 검증만 수행하는 구조로 극적 경량화.
  3. **데이터베이스 & 파일 저장**: **`Supabase PostgreSQL & Storage`** ($0/월) - Postgres DB(500MB 무료 - 100만 건 데이터 적재 가능) 및 상품 이미지 스토리지(1GB 무료) 확보.
  4. **동시성 락 캐시**: **`Upstash Serverless Redis`** ($0/월) - 분산 락에 필요한 Redis를 서버리스로 연동 (하루 10,000회 요청 무료).
  5. **코어 비즈니스 엔진**: **`Spring Boot (Render/Railway/Fly.io 등 PaaS 프리티어)`** ($0/월)
* **무료 티어 제약 극복 방안 (Pro-Tip)**:
  * *대책*: **UptimeRobot** 또는 **Cron-Job.org** 같은 무료 핑(Ping) 자동화 서비스를 설정하여, **매일 아침 API 엔드포인트에 1회 GET 요청을 쏘도록 예방책을 수립**합니다. 이로써 DB가 정지되지 않고 면접관들에게 365일 24시간 항상 상시 응답 가능하게 수호합니다.

---

### 📌 결정 9: 정적 리소스(이미지) 최적화 서빙 및 다중 레이어 보안(Multi-Layer Security) 체계 구축
* **결정 내용**: 이미지 리소스의 효율적 전달과 해킹 시나리오별 강력한 대응력을 갖추기 위해 다음 리소스/보안 스펙을 전격 채택합니다.
* **1. 정적 리소스(이미지) 아키텍처 및 서빙 전략**:
  * **Public vs Private Bucket 격리**:
    * `Public Bucket (상품 이미지)`: 비로그인 유저도 즉시 조회할 수 있고 전 세계 CDN 캐싱을 적용해 속도를 극대화합니다.
    * `Private Bucket (결제 영수증, 정산서 등 민감 데이터)`: RLS 정책을 통해 본인 외에 절대 접근을 차단하며, 필요한 경우에만 유효기간 15분짜리 **임시 서명된 URL(Signed URL)**을 발급하여 보안 서빙합니다.
  * **이미지 2단계 최적화 및 10MB 업로드 한도**:
    * *원본 크기 한도 완화*: 최근 스마트폰 카메라 해상도 향상으로 원본 사진 크기가 2MB를 쉽게 상회하므로, 관리자의 편리한 원본 업로드를 위해 **최대 업로드 제한을 10MB로 완화**합니다.
    * *클라이언트 사이드 압축(1단계)*: 프론트엔드(Admin SPA)에서 업로드 직전 WebP 포맷 변환 및 가벼운 압축(200KB~500KB 수준)을 선행 수행해 네트워크 전송 및 Supabase 스토리지 용량을 절약합니다.
    * *서버 사이드 리사이징(2단계)*: 브라우저에 서빙할 때는 Supabase Image Transformation 기술을 활용하여 화면 해상도에 최적화된 크기만 잘라서 초고속 전송합니다.
  * **악성 파일 업로드 가드**: 파일 업로드 시 파일 확장자(화이트리스트: `jpg, jpeg, png, webp`만 허용), Content-Type 검증 및 파일 용량 10MB 제한을 Supabase Storage 정책 단에서 강제하여 웹쉘(WebShell) 삽입 공격을 원천 방어합니다.
* **2. API & DB 다중 보안 아키텍처**:
  * **CORS 및 BFF-백엔드 간 비밀 키 검증 (BFF Tunneling)**: Next.js BFF와 스프링 백엔드 사이에 **프라이빗 공유 키(X-Internal-BFF-Key)** 헤더 검증 필터를 구축합니다. 이로 인해 해커가 스프링 백엔드 API 주소를 직접 공격해도 즉시 차단됩니다.
  * **데이터베이스 행 레벨 보안 (RLS - Row-Level Security) 강제**: Supabase DB 테이블 전체에 RLS를 활성화하고, 주문 테이블의 경우 `auth.uid() = user_id` 조건문 검증을 적용해 타인의 데이터 조회를 데이터베이스 레벨에서 원천 차단합니다.
