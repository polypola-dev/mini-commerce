# k8s — 로컬(kind) 및 매니페스트

k8s 전환(ROADMAP G계열, GH #9 에픽) 산출물 디렉토리.

## 구성

- `kind/cluster.yaml` — 로컬 kind 클러스터 정의 (G1, ADR-008)
- `base/` — 환경 무관 공통 매니페스트: 4개 서비스 Deployment+Service, ConfigMap (G2·G3, ADR-009)
- `overlays/local/` — kind 대상 오버레이 + 로컬 전용 postgres/redis StatefulSet+PVC (G4, ADR-010), `overlays/prod/` — OKE 대상 오버레이
- `kafka/` — Strimzi operator values + Kafka 클러스터 CR (G5, ADR-011)
- `ingress-nginx/` — ingress-nginx 컨트롤러 Helm values (G6, ADR-012) — 라우팅 규칙은 `base/ingress.yaml`
- `doc/` — k8s 관련 ADR·문서

## 로컬 배포 (전체 순서)

```bash
# 1. 클러스터 생성 (최초 1회)
kind create cluster --config k8s/kind/cluster.yaml

# 2. Kafka — Strimzi operator(Helm) + 클러스터 CR (postgres/redis는 overlays/local에 포함됨)
helm repo add strimzi https://strimzi.io/charts/
helm install strimzi-operator strimzi/strimzi-kafka-operator \
  --version 1.1.0 -n strimzi --create-namespace -f k8s/kafka/operator-values.yaml
# (namespace가 아직 없다고 나오면 kubectl apply -k k8s/overlays/local을 먼저 한 번)
kubectl apply -f k8s/kafka/kafka-cluster.yaml
# shop-api는 부팅 시 bootstrap DNS 해석이 필요하므로(G3 발견) Kafka CR을 앱보다 먼저 적용
kubectl wait kafka/mini-commerce -n mini-commerce --for=condition=Ready --timeout=600s

# 3. Secret 생성 (실물 미커밋 — G8에서 관리 방식 확정)
set -a && source .env && set +a
kubectl create secret generic app-secrets -n mini-commerce \
  --from-literal=DATABASE_USERNAME=minicommerce \
  --from-literal=DATABASE_PASSWORD=minicommerce \
  --from-literal=REDIS_PASSWORD='' \
  --from-literal=BFF_SECRET_KEY="$BFF_SECRET_KEY" \
  --from-literal=SUPABASE_SERVICE_ROLE_KEY="$SUPABASE_SERVICE_ROLE_KEY"
# (namespace가 아직 없다고 나오면 kubectl apply -k k8s/overlays/local을 먼저 한 번)

# 4. 이미지 빌드·주입 (compose가 빌드 담당)
docker compose build shop-api order-api order-admin order-batch
for img in mini-commerce-shop-api mini-commerce-order-api mini-commerce-order-admin mini-commerce-order-batch; do
  kind load docker-image "$img:latest" --name mini-commerce
done

# 5. Ingress 컨트롤러 (G6 — host 80/443은 kind cluster.yaml이 control-plane에 매핑)
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --version 4.15.1 -n ingress-nginx --create-namespace \
  -f k8s/ingress-nginx/values-local.yaml

# 6. 배포
kubectl apply -k k8s/overlays/local
kubectl get pods -n mini-commerce -w
```

배포 후 API 진입점은 `http://localhost`(80) 하나다 — 경로 라우팅은 `base/ingress.yaml`:
`/api/admin/orders`→order-admin, `/api/orders`→order-api, 그 외 `/api/**`→shop-api
(`/api/admin/products`는 catalog admin이라 shop-api로 감), `/internal/**`은 비노출.
프론트(BFF)를 k8s 백엔드에 붙이려면 `frontend/.env.local`에서 포트 직결 대신:

```bash
API_BASE_URL=http://localhost
ORDER_SERVICE_URL=http://localhost
ORDER_ADMIN_SERVICE_URL=http://localhost
NEXT_PUBLIC_API_BASE_URL=http://localhost  # 브라우저 직접 호출분 — 빌드타임 인라인이라 별도 명시
```

매니페스트 빌드 결과만 보려면 `kubectl kustomize k8s/overlays/local`.

## 로컬 클러스터 확인·모니터링

kind 자체에는 대시보드가 없다. Docker Desktop의 Kubernetes 탭은 내장 클러스터 전용이라
이 클러스터는 안 보인다(Containers 탭에 노드 컨테이너 3개로만 나타남). 메트릭·로그까지
포함한 정식 모니터링(Grafana, kube-prometheus-stack)은 H계열에서 도입한다.

### k9s (터미널 TUI, 설치됨)

별도 pane에서 실행 — 파드 상태·로그·이벤트를 실시간 탐색:

```bash
k9s -n mini-commerce
```

| 키 | 동작 |
|---|---|
| `Enter` / `l` | 파드 진입 / 로그 보기 |
| `d` | describe (probe 상태·이벤트) |
| `:svc` `:deploy` `:events` | 리소스 종류 전환 |
| `0` | 전체 네임스페이스 |
| `Ctrl+d` | 파드 삭제 (graceful shutdown → 재생성 관찰용) |
| `?` / `:q` | 도움말 / 종료 |

### kubectl 원라이너

```bash
# 앱 파드 상태 (READY/RESTARTS가 첫 확인 지점)
kubectl get pods -n mini-commerce

# 최근 이벤트 시간순 — 스케줄링 실패·probe 실패·OOMKill이 여기 먼저 나타난다
kubectl get events -n mini-commerce --sort-by=.lastTimestamp | tail -20

# 특정 서비스 로그 추적 / 직전 크래시 로그
kubectl logs -n mini-commerce deploy/shop-api -f
kubectl logs -n mini-commerce deploy/shop-api --previous

# probe 실패 원인 등 파드 상세
kubectl describe pod -n mini-commerce -l app.kubernetes.io/name=shop-api

# 호스트에서 API 직접 호출 (포트포워딩)
kubectl port-forward -n mini-commerce svc/shop-api 18080:8080
curl http://localhost:18080/actuator/health/readiness

# 노드별 파드 배치 확인 (멀티노드 스케줄링)
kubectl get pods -n mini-commerce -o wide
```

## 로컬 클러스터

```bash
# 생성 (control-plane 1 + worker 2, kubectl context: kind-mini-commerce)
kind create cluster --config k8s/kind/cluster.yaml

# 확인
kubectl get nodes --context kind-mini-commerce

# 삭제
kind delete cluster --name mini-commerce
```

주의:

- Docker Desktop VM 메모리를 docker compose 스택과 공유한다. 백엔드 4개 서비스를
  클러스터에 배포해 검증할 때는 compose 스택을 내리고 진행할 것.
- 호스트 80/443이 control-plane에 매핑돼 있다(G6 ingress-nginx 용). 해당 포트를
  쓰는 다른 프로세스와 충돌 주의.
- 로컬 빌드 이미지는 `kind load docker-image <image>`로 클러스터에 주입한다.

앱 쪽 설정 계약(환경변수, 리소스 산정, graceful shutdown 시간 사슬)은
`backend/doc/CONFIGURATION.md` 참조.
