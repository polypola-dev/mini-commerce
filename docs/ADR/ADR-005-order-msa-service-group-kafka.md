# ADR-005: order MSA 전환 — 서비스그룹(b) 전략 + Kafka 이벤트 externalization + 하이브리드 재고 사가

- 상태: 승인 (2026-07-02) — 재고 전략(하이브리드 재고 사가) 부분은 ADR-019(전략 c)로 대체됨
- 관련: ADR-004 일부 대체(단일 DB / "인접부만 모듈러 모놀리스" 부분 한정)

## 컨텍스트

ADR-004로 order 도메인 순수화 + 멀티모듈(모듈러 모놀리스, 단일 DB)까지 완료(Phase 0~5)했다.
그러나 Phase 6에서 벽에 부딪혔다:

- order-admin / order-batch를 **별도 프로세스**로 띄우면, 거기서 발행한 `OrderPaid` / `OrderPlaced`
  이벤트를 notification(shop-api 프로세스)이 **in-process로 못 받는다**. Modulith의
  `event_publication` 테이블은 "같은 컨텍스트에 등록된 리스너"를 재시도하는 것이라, DB를 공유해도
  다른 프로세스의 리스너에는 전달되지 않는다.
- 결과적으로 shop-api에 order 웹 진입점만 남기고 서버만 쪼개는 **애매한 중간 상태**가 됐다.
  "하나의 최상위 디렉토리 = 완결된 컨텍스트" 목표와도 어긋난다.

MSA로 갈지, 간다면 **어디까지** 쪼갤지 재확정이 필요했다.

## 결정

1. **order를 독립 배포 서비스로 자립(MSA)한다.** 단 도메인 전수 분리(모든 컨텍스트가 각자 DB, (c))가
   아니라, **트랜잭션 고응집 컨텍스트를 묶는 서비스그룹(b) 전략**을 택한다:
   **order + inventory를 한 서비스 + 한 DB로 묶어 `reserve/confirm/release`를 로컬 트랜잭션으로
   유지**한다. (DB-per-service의 진짜 이유는 부하가 아니라 결합/소유권이며, 고응집 두 도메인을
   쪼개면 매 주문이 분산 사가가 되어 비용만 크다.)
2. **inventory는 별도 Gradle 모듈로 유지**(ArchUnit으로 order↔inventory 경계 강제)하되
   **order-service에 함께 배포**한다. 모듈 ≠ 배포단위. 경계 규율을 지금 세워두면 후속 (c) 추출이 싸다.
3. **이벤트 externalization은 직접 아웃박스 구현 대신 Spring Modulith를 활용**한다:
   `event_publication` = 아웃박스, `@Externalized`로 **Kafka(KRaft 모드)** 발행,
   notification은 `@ApplicationModuleListener`에서 **`@KafkaListener`(멱등)**로 전환.
   order-batch가 `IncompleteEventPublications` 스윕으로 미발행분 복구.
4. **재고 사가는 하이브리드**: `reserve` = 동기(Redis Lua 게이트 + TTL 10분), `confirm` = `OrderPaid`
   이벤트, 복원 = `PaymentFailed`(즉시) + `ExpiredReservationReleaser` 리퍼(미신호 백스톱).
   Redis 선예약(동시성/오버셀)과 사가(경계 일관성)는 **직교** — Redis가 사가를 대체하지 않는다.
   TTL은 재고를 자동 복구하지 않으며(예약 해시만 만료), 재고 반환은 리퍼가 한다.
5. **취소 모델 정정**: 간편결제로 `PENDING_PAYMENT` 사용자취소는 사실상 무의미. 결제 전 복원 =
   `PaymentFailed`(자동) + 리퍼(이탈), **결제 후 주문취소 = PG 환불 + 확정재고 재입고 별도 에픽**.
   `EXPIRED` 상태 추가, 리퍼가 재고뿐 아니라 주문 상태까지 전이하도록 보완.
6. **catalog는 order의 동기 read dependency**로 두고 shop-api 잔류 + REST 조회(가격/상품명 스냅샷은
   이미 주문 라인에 복사). catalog 추출은 후속 옵션.

## 대안

- **(c) 도메인 전수 분리(order / inventory / catalog 각 DB)**: 가장 정석이나 order+inventory 고응집을
  쪼개 매 주문이 분산 사가(원격 reserve 멱등, 고아 hold)가 됨. 학습가치는 크지만 현 시점 과투자 →
  **후속 에픽**으로 미룸(학습 동기는 그때 충족).
- **(a) 단일 DB 공유 + 프로세스만 분리**: 배포는 나뉘고 DB 결합은 유지 → 분산 모놀리스 안티패턴, 기각.
- **직접 아웃박스(테이블 + AFTER_COMMIT + 배치) 구현**: Modulith가 이미 동일 기능을 제공하는데 중복.
  학습 목적이 아니면 불필요 → 기각.
- **코레오그래피 재고 사가(전부 이벤트, AWAITING_STOCK 비동기 확정)**: Redis 동기 게이트가 이미
  즉시 accept/reject를 주므로 비동기 확정은 과함 + 주문 UX가 비동기로 바뀜 → 기각.

## 결과

- order+inventory가 로컬 트랜잭션을 유지해 **분산 사가 복잡도를 회피**한다.
- 사용자의 원래 이벤트 비전(outbox → Kafka → notification / batch)이 **아웃바운드**로 그대로 실현된다.
- DB는 **order-db + shop-db**(cart/review/notification/catalog) **2분할로 시작**, 필요 시 확장.
  4-DB를 강제하지 않는다.
- **ADR-004의 "단일 DB 유지" 및 "order 인접부만 모듈러 모놀리스에 머무름" 결정을 supersede.**
  단 ADR-004의 도메인 순수화·모듈 경계 컴파일타임 강제·"모듈은 자기 테이블만 소유" 규율은 **그대로
  유효**하며 오히려 MSA의 전제가 된다.
- 트레이드오프: Kafka 운영요소 추가(KRaft로 경량화), 분산 사가 학습은 후속으로 이연, catalog 원격
  read로 order의 catalog 가용성 의존 발생.
- **트레이드오프(S3-3b 확정)**: catalog(shop-api)와 order-api가 서로를 REST로 동기 호출한다
  (catalog→order-api: 재고 배치조회, order-api→catalog: 상품/옵션 조회) — 양방향 동기 의존은
  distributed monolith 성격이 있다. 코드 결함은 아니며, 두 서비스 중 하나가 다운되면 다른 쪽의
  일부 기능(재고 표시/상품 스냅샷 조회)이 저하되는 정도로 범위가 제한적이라 지금 단계에서는
  수용. 후속 완화책 후보: 캐시/서킷브레이커 도입, 또는 옵션 B(order-api가 자체 재고 read model을
  갖고 catalog 이벤트로 동기화)로 전환.
- 진행 체크리스트 = **GitHub Issue #2**(Stage S1~S4, 완료). 후속 에픽은 별도 이슈로 분리:
  **#3**(inventory 분산 사가 추출, 전략 c), **#4**(결제 후 주문취소 PG 환불), **#5**(order-events
  계약 모듈 추출), **#6**(재고 조회 write-on-read 재검토). 목표 구조/사가 다이어그램 =
  `backend/doc/architecture/{order,inventory}.md`(mermaid).

## 관련 세션
- [[sessions/2026-07-02]]
- supersedes (부분) [[ADR/ADR-004-order-hexagonal-multimodule-msa]]
- GitHub Issue #2 (후속 진행), #1 (close)
