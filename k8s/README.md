# k8s — 로컬(kind) 및 매니페스트

k8s 전환(ROADMAP G계열, GH #9 에픽) 산출물 디렉토리.

## 구성

- `kind/cluster.yaml` — 로컬 kind 클러스터 정의 (G1, ADR-008)
- `base/` — 환경 무관 공통 매니페스트: 4개 서비스 Deployment+Service, ConfigMap (G2·G3, ADR-009)
- `overlays/local/` — kind 대상 오버레이, `overlays/prod/` — OKE 대상 오버레이
- `dev/` — 로컬 검증용 임시 인프라(postgres/redis/kafka, emptyDir 휘발) — G4/G5에서 대체
- `doc/` — k8s 관련 ADR·문서

## 로컬 배포 (전체 순서)

```bash
# 1. 클러스터 생성 (최초 1회)
kind create cluster --config k8s/kind/cluster.yaml

# 2. 임시 인프라 (postgres/redis/kafka)
kubectl apply -f k8s/dev/postgres-redis.yaml -f k8s/dev/kafka.yaml

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

# 5. 배포
kubectl apply -k k8s/overlays/local
kubectl get pods -n mini-commerce -w
```

매니페스트 빌드 결과만 보려면 `kubectl kustomize k8s/overlays/local`.

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
