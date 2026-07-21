# ADR-021: 서비스간 동기 REST 서킷브레이커 — Resilience4j + 폴백 캐시

- 상태: 승인 (2026-07-22)
- 관련: ADR-005(order MSA 분리 — 동기 REST 간선의 출발점), ADR-019(inventory 완전분리로
  동기 간선 확대), ADR-018(관측성 — 서킷 상태는 OTel 메트릭으로 관측), ADR-020(`/internal`
  인증 — 같은 REST 간선의 보안 짝), ROADMAP D6, backend/doc/CONFIGURATION.md(env var 계약)

## 컨텍스트

MSA 분리(ADR-005) + inventory 완전분리(ADR-019)로 **서비스간 동기 REST 간선이 늘었고, 어느
한쪽도 상대 장애로부터 격리돼 있지 않았다.** 타임아웃조차 없어(기본 read timeout 무한) 상대가
응답 없이 매달리면 호출자의 요청 스레드가 그대로 묶였다.

대상 간선(3개):

| 간선 | 호출 성격 | 상대 다운 시 기존 동작 |
|---|---|---|
| catalog(shop-api) → inventory-api | 상품 목록/상세의 재고 조회 | 상품 페이지 전체가 재고 조회 하나 때문에 멈춤/오류 |
| order-infra → catalog(shop-api) | 주문 금액 계산의 상품/옵션 조회 | 주문 생성이 catalog 장애로 실패 |
| order-infra → inventory-api | 재고 예약/해제 사가 | 이미 `InventoryUnavailableException`→503은 있으나 무한 대기 여지 |

한쪽 다운이 호출자로, 다시 그 호출자로 번지는 **연쇄 장애**가 구조적으로 열려 있었다.

## 결정

**Resilience4j 서킷브레이커 + 연결/읽기 타임아웃(2s)을 3간선에 도입한다.** 폴백 전략은
간선의 데이터 성격에 따라 둘로 갈린다.

| 간선 | 폴백 | 근거 |
|---|---|---|
| catalog → inventory (조회) | **폴백 캐시** — Redis에 마지막 성공값, 미적중은 0(품절) | 재고는 부가 정보. 장애 중에도 상품 페이지는 떠야 한다 |
| catalog → inventory (어드민 쓰기) | **없음** | 쓰기가 조용히 성공한 척하면 운영자가 반영된 줄 안다 — 실패를 드러낸다 |
| order → catalog | **없음(빠른 실패 503)** | 주문 금액의 입력. 낡거나 추측한 가격으로 진행하면 잘못된 금액이 확정된다 |
| order → inventory | **없음(기존 503 계약 유지)** | 재고 예약은 추측 불가. `InventoryUnavailableException`으로 감싸 기존 사가 매핑 재사용 |

### 서킷 통계에서 제외하는 예외 (`ignoreExceptions`) — 가장 중요한 판단

품절·없는 상품·예약 충돌은 **상대 서비스가 정상 동작 중이라는 증거**다. 이를 실패로 세면:

- **품절**(`OutOfStockException`)을 세면 → 인기 상품 하나가 품절된 것만으로 서킷이 열려
  **다른 모든 상품의 주문까지 끊긴다.** 가장 치명적인 오작동이라 최우선 제외 대상.
- **404**(없는 상품 → `EntityNotFoundException`)를 세면 → 잘못된 productId를 반복 조회하는
  것만으로 멀쩡한 catalog가 끊긴다.
- **예약 충돌·잘못된 요청**(`IllegalStateException`, 409/400)은 요청 조립 문제이거나 정상
  흐름 — 어느 쪽도 재고 서비스의 건강 상태가 아니다.

서킷 실패로 세는 것은 **연결 실패/타임아웃/5xx**(`InventoryUnavailableException`,
`ResourceAccessException`)뿐이다.

### 설계 세부와 근거

- **AOP 자기호출 함정 → `InventoryStockGateway` 분리.** `@CircuitBreaker`는 스프링 AOP
  프록시로 동작하므로 **같은 클래스 안의 자기호출은 프록시를 타지 않아 무효**가 된다. 기존
  `InventoryClient.availableStock(id, default)`는 같은 클래스의 `availableStocks(...)`를 부르고
  있었으므로, REST 호출부를 별도 빈(`InventoryStockGateway`)으로 옮겨 모든 진입 경로가
  프록시를 지나게 했다. `InventoryClient`는 공개 시그니처를 유지하는 얇은 위임 계층으로 남긴다.
- **`registerHealthIndicator: false`** — 서킷 상태를 readiness/health에 넣지 않는다. 넣으면
  상대 순단 시 **전 레플리카가 동시에 LB에서 빠져** 재고 조회 하나 때문에 상품/주문 계층
  전체가 죽는 연쇄 장애가 된다. F2의 "readiness는 외부 의존 미포함" 원칙과 정합 —
  서킷 상태는 OTel 메트릭으로만 관측한다.
- **폴백 메서드의 예외 타입을 `CallNotPermittedException`으로 좁힘(order 측).** 폴백을
  `Throwable`로 넓게 받으면 `ignoreExceptions` 대상 예외까지 폴백에 삼켜져 503으로 둔갑한다.
  order→catalog/inventory 폴백은 서킷이 열린 경우(`CallNotPermittedException`)에만 걸어,
  무시 대상 예외는 원래 타입 그대로 전파되게 했다.
- **폴백 캐시 네임스페이스 격리 — `catalog:stock:*`.** inventory-api가 원장으로 쓰는
  `stock:*`/`reservation:*`와 같은 Redis를 공유하므로, 캐시는 자기 네임스페이스 밖을 절대
  읽거나 쓰지 않는다 — **재고 원장을 캐시가 덮어쓰는 사고를 구조적으로 차단.** 캐시 read/write
  실패는 전부 삼켜 미적중과 동일 취급(폴백용 캐시가 새 장애원이 되면 목적이 뒤집힌다).
- **폴백 미적중은 0(품절 표시).** 재고를 모르는 상태에서 낙관적 수를 보이면 주문 단계에서
  품절로 뒤집히므로 보수적인 쪽을 택한다. 상품 테이블의 `stock` 컬럼은 원장과 동기화되지
  않은 낡은 값이라 폴백 대상이 아니다(요청 id는 값이 없어도 키를 채워 반환해 그 낡은 값으로
  새지 않게 한다).
- **타임아웃 2s / 캐시 TTL 1h.** 조회는 사용자 요청 경로 한복판이라 무한 대기를 막는다.
  캐시 TTL은 "오래된 재고를 오래 보여줌" vs "장애가 길면 폴백 무력화"의 절충.
- **버전 단일 소유.** `resilience4j-spring-boot3` 좌표는 Spring Boot BOM 관리 밖이라
  `backend/build.gradle`에서 버전(2.3.0)을 단일 소유한다. `@CircuitBreaker`가 AOP라
  `starter-aop`를 함께 넣는다.

## 결과 — 실컨테이너 검증 (catalog → inventory)

docker-compose 격리 스택에서 서킷 **전체 수명주기**를 실증했다:

1. **정상**: inventory 원장에 재고 세팅 → `/api/products` 실값 반환, 폴백 캐시 적재.
   `catalog:stock:*`가 원장 `stock:*`과 별도 키임을 확인(네임스페이스 격리).
2. **오픈**: inventory-api STOP 후 연타 → 폴백 로그의 `cause`가
   `ResourceAccessException`(실제 연결 시도·실패 누적)에서
   `CallNotPermittedException: CircuitBreaker 'inventory' is OPEN`(연결 차단)으로 전이.
   전 구간 HTTP 200 + 캐시값 응답, 500 없음.
3. **복구**: inventory-api 재기동 + 원장값 변경 → `waitDurationInOpenState`(10s) 경과 후
   HALF_OPEN→CLOSED, 조회가 캐시값이 아닌 **실값**을 반환(라이브 호출 재개).

## 한계 (의식적으로 수용)

- **order 측 간선(order→catalog / order→inventory)은 단위 테스트 레벨.** 실트래픽 트리거에
  주문 결제(JWT 인증) 경로가 필요해 실컨테이너 실증은 kind 통합배포 시 실주문으로 미룬다.
  특히 **`ignoreExceptions`로 품절이 서킷을 안 여는 판단**은 이 간선에 있어 실트래픽 실증이
  아직 없다(설정·어댑터 단위 테스트로만 커버).
- **서킷 상태 메트릭 노출은 별건.** 검증은 폴백 로그의 `cause` 전이로 했다. Prometheus에
  `resilience4j_circuitbreaker_state`를 노출하는 management 설정은 H계열 대시보드에 얹을 때
  함께 본다.
- **버스트 트래픽 튜닝 미검증.** `slidingWindowSize`/`minimumNumberOfCalls`/`failureRate`는
  보수적 기본값이며, 실부하(E6) 데이터로 재조정 여지를 남긴다.
