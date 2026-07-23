# Argo CD — GitOps CD (G11 2단계, ADR-022)

운영 클러스터(OKE)에 App-of-Apps 패턴으로 mini-commerce 전체 스택을 배포·동기화한다.
1단계(GitHub Actions kind 배포 검증, ADR-016)의 후속 — 이제 배포 주체가 CI push가
아니라 **클러스터 안의 Argo CD가 Git을 pull**하는 방식으로 바뀐다.

## 구조

```
k8s/argocd/
  install/values.yaml          Argo CD 자체 설치(Helm) — ksops CMP + Kafka health
  projects/mini-commerce.yaml  AppProject (소스·목적지 RBAC 경계)
  bootstrap/root.yaml          App-of-Apps 루트 (수동 apply하는 유일한 Application)
  apps/                        root가 관리하는 child Application들
    00-strimzi-operator.yaml   wave 0  Helm (strimzi)
    00-ingress-nginx.yaml      wave 0  Helm (ingress-nginx) — values-prod.yaml 필요
    10-kafka-cluster.yaml      wave 1  kustomize (k8s/kafka, Kafka CR만)
    20-mini-commerce.yaml      wave 2  kustomize (k8s/overlays/prod)
```

**sync-wave 순서 = 부팅 의존성 계약**: operator/ingress(0) → Kafka CR(1) → 앱(2).
앱이 Kafka Ready 전에 뜨면 bootstrap DNS 크래시(G3)라, wave 1의 Kafka health가
Healthy가 될 때까지 wave 2가 대기하도록 install/values.yaml에 health check를 넣었다.

## 부트스트랩 순서 (OKE kubeconfig 확보 후)

```bash
# 0. 운영 age 키를 Argo에 주입 (Secret 복호화용, ADR-013/022)
kubectl create namespace argocd
kubectl create secret generic sops-age -n argocd \
  --from-file=keys.txt=$HOME/.config/sops/age/keys.txt   # ⚠️ 운영 전용 키 권장

# 1. Argo CD 설치 (Helm)
helm repo add argo https://argoproj.github.io/argo-helm
helm install argocd argo/argo-cd -n argocd \
  --version <pin> -f k8s/argocd/install/values.yaml

# 2. AppProject → 루트 순으로 apply (root가 project를 참조하므로 project 먼저)
kubectl apply -f k8s/argocd/projects/mini-commerce.yaml
kubectl apply -f k8s/argocd/bootstrap/root.yaml

# 3. 이후는 전부 Git 커밋으로 반영. UI 확인:
kubectl -n argocd port-forward svc/argocd-server 8080:443
# admin 초기 비번: kubectl -n argocd get secret argocd-initial-admin-secret ...
```

## OKE 확보 후 채워야 할 TODO (현재 스캐폴드의 빈칸)

1. `k8s/ingress-nginx/values-prod.yaml` — service.type=LoadBalancer(OCI 무료 LB), OCI 애노테이션
2. `k8s/overlays/prod/` — GHCR arm64 이미지 태그(E4 산출물), ingress host/TLS patch,
   KSOPS generator(prod Secret), configmap prod 값
3. `k8s/secrets/prod-app-secrets.enc.yaml` — 운영 비밀(sops 암호화 커밋, .env 원천 분리)
4. Argo CD 버전 pin, ksops 이미지 태그 확정, 접속 노출 전략, admin 비번 회전
5. 이미지 태그 갱신 흐름 결정 — CI가 Git 태그를 write-back vs Argo Image Updater (ADR-022 참조)

## 관측성(H계열)은 별도

kube-prometheus-stack을 이 App-of-Apps에 넣을지는 OKE 리소스(4 OCPU/24GB 공유)
여유와 Grafana Cloud 대안 결정에 달려 있어 **stage 2 초기 범위에서 제외**한다(ADR-022).
넣기로 하면 `apps/30-observability.yaml`(wave 3)로 추가한다.
