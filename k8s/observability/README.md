# k8s 관측성 스택 (H1~H3, ADR-018)

kind 전용. `monitoring` 네임스페이스에 Helm으로 설치한다. compose 시절 스택
(저장소 루트 `observability/README.md`)의 k8s 승계 버전 — 컴포넌트·원칙은 대부분
동일하고, 앱→Collector→백엔드 경로가 추가된 점이 다르다.

## 설치

```bash
kubectl apply -f k8s/observability/namespace.yaml

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo update

helm upgrade --install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --version 87.15.2 -n monitoring -f k8s/observability/kube-prometheus-stack-values.yaml

helm upgrade --install tempo grafana/tempo \
  --version 1.24.4 -n monitoring -f k8s/observability/tempo-values.yaml

helm upgrade --install loki grafana/loki \
  --version 6.55.0 -n monitoring -f k8s/observability/loki-values.yaml

helm upgrade --install otel-collector open-telemetry/opentelemetry-collector \
  --version 0.165.0 -n monitoring -f k8s/observability/otel-collector-values.yaml
```

이 스택이 설치된 뒤에 `kubectl apply -k k8s/overlays/local`을 적용(또는 재적용)해야
앱이 Collector를 가리키는 `OTLP_*_ENDPOINT` 패치를 받는다.

## Grafana 접속

```bash
kubectl port-forward -n monitoring svc/kube-prometheus-stack-grafana 3000:80
```

`http://localhost:3000` — 익명 Admin 접근(로그인 불필요, compose 승계).
Datasource: Prometheus(기본) · Tempo · Loki 3종 자동 프로비저닝.

## 조회 방법

compose 시절과 동일(저장소 루트 `observability/README.md` "Grafana에서 조회하기"
참고) — Explore에서 datasource 전환. 라벨 이름은 OTel 시맨틱 컨벤션이 Loki/Prometheus에
반영되는 방식 차이로 언더스코어 표기(`k8s_namespace_name` 등)가 붙는다.

## 경로

```
앱(4개 Deployment) --OTLP(4318)--> otel-collector(k8s_attributes 프로세서로
파드/네임스페이스 메타데이터 부착) --otlphttp(시그널별)--> Tempo(4318) / Loki
Gateway(80, /otlp/v1/logs) / Prometheus(9090, /api/v1/otlp/v1/metrics)
```

## 트러블슈팅

- **`loki-chunks-cache-0`/`loki-results-cache-0`가 Pending(Insufficient memory)** —
  kind 2워커 메모리 예산 초과. `loki-values.yaml`에서 이미 두 캐시 모두 비활성화됨
  (ADR-018). 다른 이유로 재발하면 `kubectl describe nodes`로 allocatable 확인.
- **Prometheus에 앱 메트릭이 안 보인다** — `kube-prometheus-stack-values.yaml`의
  `prometheusSpec.enableOTLPReceiver: true` 적용 여부를
  `kubectl get prometheus -n monitoring -o jsonpath='{.items[0].spec.enableOTLPReceiver}'`로 확인.
- **로그에 `trace_id`가 없다** — 활성 span 밖(부팅·Kafka 리스너 로그 등)에서는 정상.
  실제 HTTP 요청 처리 중 로그에만 실린다(compose 시절과 동일 동작, 저장소 루트
  `observability/README.md` 참고).
- **Collector가 뜨는데 데이터가 안 온다** — `kubectl logs -n monitoring
  deploy/otel-collector-opentelemetry-collector`에서 exporter 에러 확인. 앱 쪽은
  `overlays/local`의 `OTLP_*_ENDPOINT` 패치가 실제로 적용됐는지
  `kubectl get deploy shop-api -n mini-commerce -o jsonpath='{.spec.template.spec.containers[0].env}'`로 확인.

## 로컬 전용, 운영(OKE) 무관

이 스택은 kind 로컬 전용이다. 운영에서 관측성 SaaS export가 필요해지면 그때
Collector의 exporter만 교체 검토(원칙은 저장소 루트 `observability/README.md`
"로컬 전용, 프로덕션 무관" 승계).
