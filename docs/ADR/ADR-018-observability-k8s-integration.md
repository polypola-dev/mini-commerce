# ADR-018: 관측성 k8s 통합 — kube-prometheus-stack + Tempo/Loki + OTel Collector

- 상태: 승인 (2026-07-15)
- 관련: ROADMAP H1/H2/H3, GH #9(k8s 이전 에픽), docker/observability/README.md(compose 시절 원 설계),
  G9(NetworkPolicy default-deny), F5(설정 계약), ADR-005~017

## 컨텍스트

kind로 애플리케이션을 이전(G계열)하면서 compose의 관측성 스택(Tempo/Loki/Prometheus/
Grafana, 앱이 OTLP를 직접 export)은 함께 옮기지 않았다. 그 결과 kind의 4개 앱은
`overlays/local`의 `OTEL_SDK_DISABLED: "true"` 패치로 SDK 자체가 꺼진 채였다(export
실패 로그가 반복 쌓이는 것을 막기 위한 임시 조치, 코드 주석에 "H1에서 제거" 명시).
관측성이 전무한 채로 운영 성격 검증(G11 CD 등)이 계속 진행되어 온 상태.

## 결정

### 배치: `monitoring` 네임스페이스, Helm 전용

G5(Strimzi)·G6(ingress-nginx)와 동일한 원칙 — 잘 관리되는 업스트림 차트는 Helm으로
설치(G4의 "로컬 단일 인스턴스엔 차트 가치 0" 판단은 Bitnami 범용 차트에 대한 것이지,
Grafana Labs/prometheus-community가 직접 유지하는 이 차트들에는 적용되지 않는다).
`mini-commerce` 앱 네임스페이스와 분리해 `monitoring` 네임스페이스에 전부 배치 —
G9 NetworkPolicy(default-deny)가 `mini-commerce` ns에만 적용되므로 **NetworkPolicy
변경이 전혀 필요 없다**(앱은 push 방향 클라이언트라 egress 무제한 정책에 이미 해당,
monitoring ns 자체엔 정책 미적용).

### H2 — kube-prometheus-stack (`k8s/observability/kube-prometheus-stack-values.yaml`)

| 결정 | 근거 |
|---|---|
| `kubeControllerManager`/`kubeScheduler`/`kubeEtcd`/`kubeProxy` 전부 비활성 | kind 컨트롤 플레인 컴포넌트는 127.0.0.1 bind라 외부 스크레이프 불가 — 기본값대로 두면 대상이 영구 down으로 잡혀 노이즈만 발생(kind의 잘 알려진 제약) |
| `defaultRules.create: false`, `alertmanager.enabled: false` | 알림 설계는 H5 소관 — 기본 알림 규칙 대량 적용은 지금 범위 밖 |
| `prometheus.prometheusSpec.enableOTLPReceiver: true` | 차트 v87.15.2가 번들한 Prometheus **v3.13.1**의 네이티브 옵션. compose(v2.55.1)가 썼던 experimental 플래그(`--enable-feature=otlp-write-receiver`)가 stable 옵션으로 승격된 결과 — docker/observability/README.md 트러블슈팅 항목의 "v3.x 승격 여부 확인" 미결 사항이 여기서 해소됨 |
| `storageSpec.volumeClaimTemplate`(standard, 2Gi), `retention: 24h` | G4 패턴 승계 — kind local-path PVC로 파드 재기동에도 데이터 생존 |
| Grafana는 **번들된 서브차트를 그대로 사용**(별도 Grafana 배포 안 함) | Prometheus datasource가 자동 등록되고, `additionalDataSources`로 Tempo/Loki를 얹기만 하면 되어 중복 리소스가 없다 |
| Grafana 익명 Admin 접근(`grafana.ini`의 `auth.anonymous`) | compose의 `GF_AUTH_ANONYMOUS_*` env 설계를 동등하게 승계 — 로컬 전용, 대시보드/데이터 비영속(README 원칙 그대로) |

### H3 — Tempo + Loki (`tempo-values.yaml`, `loki-values.yaml`)

| 결정 | 근거 |
|---|---|
| Tempo: **단일 바이너리 차트**(`grafana/tempo`), `storage.trace.backend: local`, PVC 2Gi | compose 구조 그대로 승계 — 로컬 단일 인스턴스에 오브젝트 스토리지·분산 모드 가치 없음(G4 ADR-010과 동일 판단 논리). **주의: 이 차트는 업스트림에서 deprecated 표시**(distributed 계열로의 이관 유도) — 지금은 동작에 문제없어 그대로 쓰되, 차트가 실제로 제거될 경우 재검토 대상으로 기록 |
| Loki: `deploymentMode: SingleBinary` + `storage.type: filesystem` + `useTestSchema: true`, minio 비활성 | 기본값(`SimpleScalable`)은 오브젝트 스토리지(minio)를 강제해 로컬 단일 인스턴스에 불필요한 컴포넌트가 늘어난다. `useTestSchema`는 차트가 "테스트/로컬용"으로 공식 안내하는 단축 옵션 — storage.type을 그대로 반영해 스키마를 자동 구성 |
| `chunksCache`/`resultsCache`(memcached) 비활성 | **1차 배포 실패로 발견**: `loki-chunks-cache-0`가 `FailedScheduling: Insufficient memory`로 Pending 고착. kind 2워커 메모리 예산에 로컬 데이터 규모 대비 캐시 계층의 성능 이점이 못 미쳐 배제 |
| Grafana datasource는 `loki-gateway` Service(nginx 게이트웨이, 80포트) 경유 | 게이트웨이가 `/otlp/v1/logs`를 포함한 Loki API 전체를 표준 라우팅 — SingleBinary 컴포넌트 Service(`loki`, 3100)로 직접 붙는 것보다 차트가 의도한 안정적인 진입점 |

### H1 — OTel Collector (`otel-collector-values.yaml`)

| 결정 | 근거 |
|---|---|
| `mode: deployment`, replica 1 | 이 워크로드는 4개 앱이 순수 OTLP push만 하는 구조라 DaemonSet의 본래 용도(호스트 로그 tailing·hostmetrics)가 불필요. k8sattributes 프로세서는 소스 파드 IP 기반 연관이라 Deployment 단일 인스턴스로도 정상 동작(kind 기본 CNI는 ClusterIP 라우팅에서 클라이언트 IP를 보존) |
| 이미지 `otel/opentelemetry-collector-contrib` | `k8s_attributes` 프로세서는 contrib 전용 컴포넌트 — core 이미지엔 없음 |
| `presets.kubernetesAttributes.enabled: true` | k8s 리소스 메타데이터(namespace/pod/deployment/replicaset 등) 자동 부착 + 필요 RBAC(ClusterRole: pods/namespaces get·list·watch, replicasets 동일)를 차트가 자동 생성 — H1의 본래 목적 |
| exporter 3종을 시그널별로 분리(`otlphttp/tempo`, `otlphttp/prometheus`, `otlphttp/loki`) | compose 시절 앱이 직접 했던 시그널별 endpoint 분리(F5 계약과 동일 패턴)를 Collector 레벨로 그대로 이관. Prometheus만 `metrics_endpoint`로 전체 경로(`/api/v1/otlp/v1/metrics`)를 명시 오버라이드(표준 `<endpoint>/v1/metrics` 자동 부착 경로와 다름) |
| `service.pipelines`는 `receivers`/`exporters`만 오버라이드, `processors`는 프리셋이 넣어준 `[k8s_attributes, memory_limiter, batch]`를 그대로 승계 | Helm 값 병합이 맵 단위로 이뤄져 손대지 않은 키는 프리셋 결과가 보존됨(렌더링으로 실증) — "config는 프리셋에 추가만 가능, 제거 불가"라는 차트 문서 원칙과 정합 |
| `overlays/local`의 `OTEL_SDK_DISABLED` 패치 제거 → `OTLP_TRACES_ENDPOINT`/`OTLP_METRICS_ENDPOINT`/`OTLP_LOGS_ENDPOINT`를 Collector Service(`otel-collector-opentelemetry-collector.monitoring.svc.cluster.local:4318`)의 시그널별 경로로 지정 | 코드에 미리 남겨둔 예고("H1에서 이 패치를 제거한다")를 그대로 실행 |

## 검증

1. `kubectl apply -k k8s/overlays/local` 후 4개 앱 전부 정상 롤아웃.
2. Tempo `/api/search`로 `service.name=mini-commerce-backend` 트레이스 조회 성공
   (HTTP 요청 span + JDBC 쿼리 span 포함).
3. Prometheus `target_info` 메트릭에 `k8s_namespace_name=mini-commerce`,
   `k8s_pod_name`, `k8s_deployment_name` 등 리소스 속성이 라벨로 확인됨 —
   k8s_attributes 프로세서 enrichment 실증.
4. Loki 로그 스트림에 동일한 k8s 라벨(`k8s_namespace_name`, `k8s_pod_name` 등) 부착
   확인. `trace_id`가 없는 로그 줄(부팅·Kafka 리스너 로그)은 활성 span 밖이라 정상
   (docker/observability/README.md 기존 트러블슈팅 항목과 동일한 현상 — 신규 이슈 아님).
5. Grafana `additionalDataSources`로 Prometheus(기본)·Tempo·Loki 3종 API 조회로 확인.

## 결과 및 트레이드오프 (1차, H1~H3)

- 관측성 3축이 kind에 정식으로 통합됐다 — G11 이후 비어 있던 공백 해소.
- Tempo 차트의 deprecated 상태는 미해결 리스크로 남는다 — 차트가 실제로 저장소에서
  빠지면 `tempo-distributed` 또는 후속 통합 차트로 이관 필요(그때 재평가).
- 운영(OKE) 관측성은 여전히 별도 결정 대상 — 이 스택은 로컬 전용(docker/observability/README.md
  원칙 계승, SaaS export 재검토는 그때).

## 추가 결정 (2차, 2026-07-15 — Grafana 영구 접속 + H4 + H5)

1차 배포 직후 사용자가 Grafana 접속을 `kubectl port-forward` 대신 영구적으로 요청,
그 자리에서 H4/H5까지 이어서 진행.

### Grafana 영구 접속 — Ingress 서브패스(`/grafana`)

port-forward는 kind 재생성·터미널 종료마다 재실행이 필요해 "영구적"이라는 요구와
맞지 않는다. G6 base ingress(prod 공유 파일)에는 손대지 않고, **kube-prometheus-stack
차트 자체의 `grafana.ingress`**를 활성화하는 방식을 택했다 — 같은 ingress-nginx
컨트롤러가 여러 네임스페이스의 Ingress 리소스를 동시에 서빙하므로 base와 충돌 없이
공존한다. `host` 미지정(G6 host-less 패턴과 동일 — prod에서 host/TLS patch 여지).
서브패스 서빙에는 `grafana.ini`의 `server.root_url`+`serve_from_sub_path: true`가
필수(없으면 정적 자산 경로가 깨진다) — 렌더링된 HTML의 상대경로 자산이 `/grafana/`
기준으로 정상 해석되는 것까지 실증.

### H4 — 서비스별 RED 대시보드

`k8s/observability/dashboards/service-red-dashboard.json`(소스) +
`service-red-dashboard-configmap.yaml`(배포 아티팩트, `grafana_dashboard: "1"`
라벨). kube-prometheus-stack 기본 대시보드(kubernetes-mixin 등)와 **동일한
sidecar 메커니즘**(`sidecar.dashboards.searchNamespace: ALL`, 차트 기본값)을
그대로 활용 — 별도 프로비저닝 설정 불필요, ConfigMap만 만들면 자동 로드된다.
`job` 템플릿 변수로 4개 서비스(shop-api/order-api/order-admin/order-batch) 전환,
패널: 요청률·5xx 에러율·p50/p95/p99 레이턴시·상태코드별 분포·JVM 힙·GC pause율·
파드 재시작 횟수. 쿼리는 실제 존재 확인된 메트릭만 사용
(`http_server_request_duration_seconds_*`, `jvm_memory_used_bytes`,
`jvm_gc_duration_seconds_sum`, `kube_pod_container_status_restarts_total`).
검증: Grafana API 대시보드 검색 성공 + datasource 프록시로 패널 1의 실제 쿼리가
데이터를 반환하는 것까지 확인.

### H5 — Alertmanager 알림 규칙

| 결정 | 근거 |
|---|---|
| `alertmanager.enabled: true`로 전환(1차에서 P2로 배제했던 것을 여기서 활성화) | 규칙 평가 자체엔 Alertmanager가 전제 조건 |
| `defaultRules.create: false` 유지 | kube-prometheus-stack 기본 규칙(수백 개, 클러스터 전반 대상)은 노이즈가 커 배제 — 우리 서비스에 맞춘 규칙만 별도 `PrometheusRule`로 직접 관리 |
| `PrometheusRule`에 `release: kube-prometheus-stack` 라벨 필수 | Prometheus CR의 `ruleSelector`가 이 라벨로 매칭(`ruleNamespaceSelector`는 `{}`라 전체 네임스페이스 허용 — 라벨 매칭만 관문) |
| 규칙 3종: `HighErrorRate`(5xx 비율 5% 초과, 5분), `PodRestartingFrequently`(1시간 내 재시작 3회 초과), `KafkaConsumerLagHigh`(컨슈머 랙 1000건 초과, 10분 — 3차 후속에서 추가) | ROADMAP H5 문구의 "에러율·Pod 재시작·컨슈머 랙"에 해당 |
| **receiver 미설정**(Alertmanager 기본 route만 존재, Slack/Telegram 연동 안 함) | 사용자 결정(2026-07-15): "지금은 규칙만" — webhook/봇 토큰이 준비되면 그때 receiver 추가. 지금은 firing 여부를 Alertmanager/Grafana UI에서 확인하는 것까지가 스코프 |

검증: `PrometheusRule` 적용 후 Prometheus `/api/v1/rules`에 두 그룹 모두
`health=ok`로 로드(현재는 임계 미도달로 `inactive` — 정상), `/api/v1/alertmanagers`로
Alertmanager가 활성 타겟으로 자동 발견됨(오퍼레이터가 기본 연동, 별도 설정 불필요)
확인.

## 추가 결정 (3차, 2026-07-15 — RED 대시보드 버그 2건 + Kafka Exporter)

### 대시보드 버그 발견·수정 (사용자가 k9s로 파드 강제 Kill 후 발견)

1. **JVM heap "No data"**: 패널 쿼리가 `area="heap"`로 필터링했는데 실제 라벨은
   `jvm_memory_type="heap"`(OTel Java 계측 관례 — 클래식 Micrometer Prometheus
   레지스트리의 `area`와 다름). 라벨명 수정으로 해결.
2. **`$job` 변수가 Container restarts/Pod age 패널에 안 먹힘**: 이 두 패널은
   kube-state-metrics 메트릭(`kube_pod_container_status_restarts_total`,
   `kube_pod_start_time`)을 쓰는데, `job` 라벨 도메인이 OTel 앱 메트릭과 아예
   다르다(`job="kube-state-metrics"`). 게다가 OTel `job` 값 자체도 서비스마다
   일관성이 없었다(shop-api의 `OTEL_SERVICE_NAME` 기본값이 `mini-commerce-backend`라
   Deployment 이름 `shop-api`와 다름). **해결**: 템플릿 변수를 `job` 대신 k8s
   Deployment 이름 도메인(`target_info`의 `k8s_deployment_name` — shop-api/
   order-api/order-admin/order-batch, 전부 일관)으로 교체. OTel 기반 패널은
   `* on(instance, job) group_left(k8s_deployment_name) target_info` 조인으로
   `k8s_deployment_name`을 얻고, kube-state-metrics 기반 패널은 같은 변수값을
   `pod` 라벨 접두사 정규식(`pod=~"($workload)-.*"`)으로 필터 — 하나의 변수로
   10개 패널 전부 일관되게 필터링됨을 실측 확인. **Prometheus의
   `promoteResourceAttributes` 옵션으로 메트릭에 직접 라벨을 붙이는 대안 대신
   `target_info` 조인을 택함** — Prometheus CR(Operator 리소스) 변경 없이
   대시보드 쿼리만으로 해결되어 더 가볍다.

### Kafka Exporter (컨슈머 랙 관측)

| 결정 | 근거 |
|---|---|
| Strimzi 내장 `spec.kafkaExporter` 옵션 사용(`k8s/kafka/kafka-cluster.yaml`) | 별도 컴포넌트 설치·이미지 관리 불필요 — Kafka CR 필드 하나로 Deployment 생성됨 |
| **PodMonitor**(Service 아님) 경유로 Prometheus 연결 | Strimzi가 kafka-exporter의 Service는 만들어주지 않고 파드만 생성 — Service 없이 파드 라벨(`strimzi.io/name=mini-commerce-kafka-exporter`)로 직접 스크레이프 |
| PodMonitor에 `release: kube-prometheus-stack` 라벨 필수 | Prometheus CR의 `podMonitorSelector`가 이 라벨로 매칭(H5의 `ruleSelector`와 동일 패턴) |
| **NetworkPolicy 변경 불필요** | Strimzi가 kafka-exporter 파드용 NetworkPolicy(`mini-commerce-kafka-exporter`)를 자동 생성해 G9 default-deny와 공존 — ADR-014에서 이미 예상했던 "Kafka는 Strimzi 자동 생성 netpol 소관" 그대로 |
| `kafka_consumergroup_lag`가 비어 있을 수 있음을 문서화(버그 아님) | order.placed/order.paid 토픽에 메시지가 한 번도 안 쌓이면(offset=0) 컨슈머 그룹의 커밋 오프셋 자체가 없어 lag 계산이 성립하지 않음 — `kafka_consumergroup_members`(그룹 인식 자체)는 정상 확인됨 |

검증: PodMonitor 적용 후 Prometheus 타겟 `health=up` 확인, `kafka_consumergroup_members{consumergroup="notification"}=2`(실제 컨슈머 수와 일치) 확인. `KafkaConsumerLagHigh` 규칙은 Prometheus에 `health=ok`로 로드됨(실제 랙 발생 전까지는 데이터 없음 — 정상). 대시보드에 Kafka 패널 2종(랙, 컨슈머 그룹 멤버 수) 추가.

## 결과 및 트레이드오프 (종합)

- Grafana는 이제 `http://localhost/grafana`로 영구 접속 가능 — kind를 재생성해도
  `helm upgrade`만 재실행하면 동일하게 재현(포트포워드 불필요).
- H4/H5까지 포함해 H계열(관측성 — k8s 통합) 5개 항목 전부 완료, 컨슈머 랙까지
  포함해 H5 문구 그대로 충족.
- Alertmanager receiver는 의도적 미완성 상태로 남는다 — Slack/Telegram webhook이
  생기면 `alertmanager.config.receivers`에 추가하는 후속 작업으로 명확히 분리.
- Kafka consumer lag는 실제 주문 트래픽이 발생해야 값이 채워진다 — 지금은 파이프라인
  연결만 검증된 상태(대시보드에 "No data가 정상"이라고 명시해 혼동 방지).
