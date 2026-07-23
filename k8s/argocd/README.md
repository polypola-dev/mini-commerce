# Argo CD — GitOps CD (G11 2단계, ADR-022)

운영 클러스터(OKE)에 App-of-Apps 패턴으로 mini-commerce 전체 스택을 배포·동기화한다.
1단계(GitHub Actions kind 배포 검증, ADR-016)의 후속 — 이제 배포 주체가 CI push가
아니라 **클러스터 안의 Argo CD가 Git을 pull**하는 방식으로 바뀐다.

## 구조

```
k8s/argocd/
  install/values.yaml          Argo CD 자체 설치(Helm) — ksops(init container) + Kafka health
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

## kind 리허설 검증 (2026-07-23)

OKE 확보 전, 기존 kind 클러스터에 Argo CD(차트 10.1.4, app v3.4.5)를 설치해 신규
기계장치를 실증했다 — 매니페스트는 클러스터 종류 무관 재사용되므로 여기서 잡은
버그는 OKE에서도 그대로 해소된다.

- ✅ **ksops 복호화 end-to-end**: repo-server 안에서 `kustomize build`가 실제
  `app-secrets.enc.yaml`을 복호화해 올바른 평문 Secret 생성(값이 secrets.sh 산출과 일치).
- ✅ **ksops 내장 kustomize 주입**: init container가 `v5.3.0+ksops.v4.5.1`를
  `/usr/local/bin`에 설치, age 키 마운트·`SOPS_AGE_KEY_FILE` 확인.
- ✅ **Kafka health check + `kustomize.buildOptions`** argocd-cm 로드 확인.
- ✅ **Application 6종 server-side dry-run** 통과(실제 Argo CRD 스키마 검증).
- 🐛 **잡은 버그**: ksops 이미지가 distroless(shell 없음)라 `/bin/sh -c` init 방식이
  CrashLoop. 정본대로 `ksops install --with-kustomize`(바이너리 직접 호출)로 수정 → 해결.

### 2부 — 실제 GitOps 관리 (C, `k8s/argocd/rehearsal-kind/`)

kind 전용 App-of-Apps(overlays/local-gitops)로 ArgoCD가 로컬 스택을 실제 adopt·관리:

- ✅ App-of-Apps 루트→child 자동 생성, **wave 게이트**(Kafka wave 1 Healthy→앱 wave 2).
- ✅ **ksops-in-sync**: sync 중 enc 복호화로 `app-secrets` 생성·ArgoCD 관리(Synced).
- ✅ **GitOps 루프**: 매니페스트 push → ~45s 자동 반영. **selfHeal**: 삭제 ConfigMap
  재생성, `kubectl scale` 2대→12s 만에 1대 복원.
- 🐛 postgres/redis StatefulSet 영구 OutOfSync(immutable VCT 기본값) → 앱 전체 selfHeal
  5분 스로틀 → `ignoreDifferences`(VCT)로 해결. **prod 미발생**(ArgoCD 관리 StatefulSet 없음).

> ⚠️ **kind 클러스터가 이제 GitOps 관리 하에 있다** — mini-commerce ns를 `kubectl`로
> 수동 변경하면 selfHeal이 되돌린다. 수동 워크플로로 복귀하려면 리허설을 걷어낸다:
> `kubectl delete -f k8s/argocd/rehearsal-kind/root.yaml` (child 앱까지 캐스케이드 삭제,
> adopt된 워크로드 자체는 남음).

> 미검증(대상 클러스터+prod 산출물 필요): OKE from-scratch 부트스트랩, prod 오버레이
> 실체(GHCR 이미지·values-prod·prod Secret). OKE 확보 후 검증 3부로 실증한다.

## kind에서 두 배포 방식 토글 (수동 kubectl ↔ ArgoCD GitOps)

kind 클러스터는 **코드 변경 없이 명령어만으로** 두 방식을 오갈 수 있다(매니페스트는 이미
git에 있음). 단 **동시 사용은 안 된다** — ArgoCD가 selfHeal로 관리 중이면 수동 `kubectl`
변경을 되돌린다. 그리고 이건 **CD(배포/동기화) 방식** 토글이지 CI(GitHub Actions 빌드·테스트)는
두 모드와 무관하게 그대로다.

**→ ArgoCD GitOps 모드로 켜기** (Argo CD 설치가 이미 있는 상태 기준 — 파일 2개 apply):
```bash
kubectl apply -f k8s/argocd/projects/mini-commerce.yaml
kubectl apply -f k8s/argocd/rehearsal-kind/root.yaml
# 이후 mini-commerce 스택은 git이 진실. 매니페스트 push→자동 반영, kubectl은 관찰용.
```

**→ 수동 kubectl 모드로 되돌리기 (detach)** — ⚠️ **나이브 삭제 금지**:
Application에 `resources-finalizer.argocd.argoproj.io`가 붙어 있어 그냥 지우면 캐스케이드로
**관리 중이던 워크로드(+adopt한 Kafka CR)까지 삭제**된다. 실제로 Kafka CR이 지워져 broker가
소멸한 사고가 있었다(cluster.id 불일치·operator 재시작으로 복구). 반드시 **컨트롤러를 먼저
멈춰** finalizer 재부착을 막고 삭제한다:
```bash
# 1. application-controller 정지 (finalizer 재부착 차단)
kubectl scale statefulset/argocd-application-controller -n argocd --replicas=0
# 2. 전 Application을 리소스 보존(orphan)하며 삭제
kubectl -n argocd patch application --all --type=merge -p '{"metadata":{"finalizers":[]}}'
kubectl -n argocd delete application --all --cascade=orphan
# 3. 컨트롤러 원복(선택 — ArgoCD를 계속 쓸 거면)
kubectl scale statefulset/argocd-application-controller -n argocd --replicas=1
# 4. 이제 평소대로 수동 배포
kubectl apply -k k8s/overlays/local
```

> OKE(운영)에선 이 토글/사고가 애초에 없다 — from-scratch로 ArgoCD가 처음부터 전부
> 생성하므로 adopt/detach 이슈 자체가 안 생긴다.

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
