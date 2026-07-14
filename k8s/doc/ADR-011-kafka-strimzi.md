# ADR-011: Kafka on k8s — Strimzi Operator 선택

- 상태: 승인 (2026-07-14)
- 관련: ADR-007(운영 OKE, 목표 비용 $0), ADR-009(Helm은 외부 컴포넌트 소비용),
  ADR-010(로컬 PG/Redis StatefulSet), ROADMAP G5, GH #9(k8s 이전 에픽)

## 컨텍스트

compose의 단일 브로커 KRaft Kafka를 k8s로 승계해야 한다. Postgres/Redis(ADR-010)와
결정 구도가 다른 지점: **무료 managed Kafka가 없어 운영(OKE)에서도 클러스터 내 직접
운영 대상**이다 — 이 결정은 로컬 전용이 아니라 운영까지 이어진다.

G3 발견 제약: shop-api의 Kafka consumer는 부팅 시 bootstrap **DNS가 해석**돼야
기동한다(브로커 다운은 재시도로 버티지만 DNS 부재는 크래시).

후보: Strimzi Operator / 단일 브로커 StatefulSet 직접 작성.

## 결정

**Strimzi Operator 1.1.0**(Helm, `strimzi` 네임스페이스) + **KafkaNodePool 단일
dual-role 노드**(KRaft, Kafka 4.3.0)로 배치한다.

### 근거 (vs 단일 StatefulSet 직접 작성)

| 기준 | Strimzi | StatefulSet 직접 작성 |
|---|---|---|
| 운영 연속성 | 운영(OKE)에서도 같은 경로 — 브로커 증설·롤링 재시작·버전 업그레이드를 operator가 수행 | listener 광고 주소·quorum 구성·롤링을 전부 수동 관리 |
| ADR-009 정합 | "외부 컴포넌트는 Helm 소비" 그대로 (G5에 Strimzi 명시 예정돼 있었음) | "직접 작성은 Kustomize" 쪽이나, Kafka는 자작 서비스가 아닌 외부 컴포넌트 |
| 학습 가치 | Operator 패턴·CRD 실전(ROADMAP이 명시한 목표) | compose 지식 재활용에 그침 |
| 오버헤드 | operator 파드 1개 상주(로컬 kind 부담) | 없음 — 가장 가벼움 |
| arm64(OKE) | quay.io/strimzi 멀티아치 지원 | 공식 apache/kafka 이미지도 지원 |

오버헤드 트레이드오프는 감수 — 브로커 힙을 512m로 제한해 로컬 총량을 상쇄.

### bootstrap 주소 계약 변경 (G3 제약의 재해석)

"Service 이름 `kafka:9092` 유지 필수"의 본질은 리터럴 이름이 아니라 **부팅 시 DNS
해석 가능**이다. `KAFKA_BOOTSTRAP_SERVERS`는 F5 계약상 env var이므로 base ConfigMap
값만 Strimzi가 생성하는 `mini-commerce-kafka-bootstrap:9092`로 변경했다. 이 서비스는
Kafka CR 적용 직후 operator가 생성하므로 **Kafka CR을 앱보다 먼저 적용**하면 충족된다
(README 배포 순서에 명시).

### 구현 세부

- **operator**: Helm `strimzi/strimzi-kafka-operator 1.1.0`, `strimzi` ns에 설치,
  `watchNamespaces: [mini-commerce]`. values는 `k8s/kafka/operator-values.yaml` 커밋
  (ADR-009 규약 — 외부 컴포넌트 values는 k8s/ 아래 컴포넌트 디렉토리).
- **클러스터 CR**: `k8s/kafka/kafka-cluster.yaml` — Strimzi 1.x는 API
  `kafka.strimzi.io/v1` 단일 버전(KRaft·node pool 전제, 구 v1beta2 미제공).
  KafkaNodePool `dual-role`(controller+broker 겸임 1노드) = compose
  `process.roles=broker,controller` 승계.
- **compose 설정 승계**: RF=1·min ISR=1 전부(`offsets/transaction.state.log` 포함),
  `group.initial.rebalance.delay.ms=0`, `auto.create.topics.enable=true`(Modulith가
  토픽을 사전 생성하지 않음 — order.placed/order.paid 자동 생성 확인).
- **영속화**: persistent-claim 2Gi(`kraftMetadata: shared` — 메타데이터·데이터 단일
  볼륨), kind standard(local-path). G4와 동일하게 파드 재생성에도 데이터 유지.
- **listener**: internal 9092 plaintext — 클러스터 내부 전용, 인증/암호화는 G9 이후
  필요시. entityOperator 생략(KafkaTopic/KafkaUser 관리 안 함).

## 검증 (2026-07-14, kind)

- operator·브로커(`mini-commerce-dual-role-0`) Ready, PVC 2Gi Bound.
- 앱 4개 새 bootstrap 주소로 재기동 — shop-api consumer group `notification`이
  order.placed/order.paid 파티션 할당(자동 생성) 확인.
- **영속성 실증**: 테스트 토픽에 메시지 기록 → 브로커 파드 삭제 → 재생성 후 토픽·
  메시지 생존, consumer가 coordinator 재발견 후 자동 복귀(F1). readiness UP,
  `/api/products` 200.

## 결과 및 트레이드오프

- `k8s/dev/` 디렉토리 소멸 — G3의 임시 인프라 전부 정식 배치로 대체 완료(G4+G5).
- 브로커 1노드라 가용성 없음(로컬·초기 운영 허용). 증설 시 KafkaNodePool
  replicas 증가 + RF 상향이 필요한데, controller/broker 역할 분리도 그때 재검토.
- operator 설치는 Helm 명령(README) — 클러스터 재생성 시 수동 1회. G11(Argo CD)에서
  선언적 관리로 흡수 검토.
- Kafka 버전은 CR에서 생략 — operator 기본(현재 4.3.0) 추종. compose(3.9.0)와
  버전이 다르지만 클라이언트(Spring Kafka)는 하위호환, 로컬 검증으로 확인됨.
  compose 스택은 당분간 3.9.0 유지(이중 환경 기간 한정).
