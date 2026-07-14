# ADR-018: 관측성 k8s 통합 — kube-prometheus-stack + Tempo/Loki + OTel Collector

- 상태: 승인 (2026-07-15)
- 관련: ROADMAP H1/H2/H3, GH #9(k8s 이전 에픽), observability/README.md(compose 시절 원 설계),
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
| `prometheus.prometheusSpec.enableOTLPReceiver: true` | 차트 v87.15.2가 번들한 Prometheus **v3.13.1**의 네이티브 옵션. compose(v2.55.1)가 썼던 experimental 플래그(`--enable-feature=otlp-write-receiver`)가 stable 옵션으로 승격된 결과 — observability/README.md 트러블슈팅 항목의 "v3.x 승격 여부 확인" 미결 사항이 여기서 해소됨 |
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
   (observability/README.md 기존 트러블슈팅 항목과 동일한 현상 — 신규 이슈 아님).
5. Grafana `additionalDataSources`로 Prometheus(기본)·Tempo·Loki 3종 API 조회로 확인.

## 결과 및 트레이드오프

- 관측성 3축이 kind에 정식으로 통합됐다 — G11 이후 비어 있던 공백 해소.
- Grafana UI는 별도 Ingress 라우팅을 추가하지 않았다(로컬 전용 조회 편의 목적이라
  `kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80`으로
  충분 — G6 base ingress는 prod에도 적용되는 라우팅 규칙이라 관측성 UI를 얹지 않는다).
- Tempo 차트의 deprecated 상태는 미해결 리스크로 남는다 — 차트가 실제로 저장소에서
  빠지면 `tempo-distributed` 또는 후속 통합 차트로 이관 필요(그때 재평가).
- H4(Grafana 대시보드 as code)·H5(Alertmanager 알림 규칙)는 P2로 범위 밖 유지.
- 운영(OKE) 관측성은 여전히 별도 결정 대상 — 이 스택은 로컬 전용(observability/README.md
  원칙 계승, SaaS export 재검토는 그때).
