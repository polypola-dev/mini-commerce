# ADR-016: CD 1단계 — GitHub Actions kind 배포 검증 (2단계 Argo CD는 OKE+E4 대기)

- 상태: 승인 (2026-07-14)
- 관련: ROADMAP G11, GH #9(k8s 이전 에픽), ADR-008(kind), ADR-009(Kustomize),
  ADR-013(Secret — CI에는 age 키 미주입), ADR-014(NetworkPolicy 회귀 검증), E4/E5(이미지 파이프라인)

## 컨텍스트

G계열 매니페스트(G1~G10)는 전부 로컬 kind에서 수동 검증해왔다 — 매니페스트·배포
플로우가 회귀해도 자동으로 잡는 장치가 없다. G11은 2단계 구상: **1단계** GitHub
Actions에서 kind 배포 검증(지금 가능), **2단계** Argo CD GitOps(운영 클러스터 필요).

제약: 운영 클러스터(OKE)는 미구축, E4(GHCR arm64 이미지 파이프라인)·E5(Dockerfile
캐시 최적화) 미완, **리포는 private라 러너가 2코어/7GB**(무료 분수 한정).

## 결정

**1단계만 지금 구축한다** — `.github/workflows/deploy-kind.yml`: kind 클러스터 생성
→ Strimzi/Kafka/ingress-nginx 설치 → 이미지 빌드·주입 → `overlays/ci` 배포 → 롤아웃
대기 → 스모크(+NetworkPolicy 회귀) 검증. **2단계(Argo CD)는 OKE 구축 + E4 완료 후
별도 착수** — 배포 대상 클러스터와 pull 가능한 이미지 레지스트리가 없는 지금은
설계만 앞당길 실익이 없다.

### 러너 제약에 따른 세부 결정

| 결정 | 근거 |
|---|---|
| CI 전용 단일 노드 kind(`kind/cluster-ci.yaml`) | 2코어/7GB 단일 VM에서 멀티노드는 재현 가치 없이 kubeadm join·노드 데몬 오버헤드만. ingress-ready·80/443 매핑 계약은 로컬과 동일 |
| `overlays/ci` = `../local` 상속 + startupProbe failureThreshold 90 | 검증 대상은 local 오버레이 그 자체. 유일한 델타는 부팅 예산(60s→180s) — 2코어에서 JVM 5개 동시 부팅은 로컬 예산 초과 가능, startupProbe가 정확히 그 흡수 장치. probe 경로·주기 계약 불변 |
| 트리거는 `k8s/**`·Dockerfile·compose·워크플로 변경 + 수동 | 이미지 4종이 매번 풀빌드(E5 미해결 — `COPY . .` 캐시 전멸)라 매 푸시 실행은 분수 낭비. 백엔드 코드는 ci.yml(gradlew build)이 이미 게이트. E5 후 매 푸시 확대 재평가 |
| Secret은 CI 더미(`kubectl create secret`) | 배포 검증에 실비밀 불필요 — JWKS·서비스롤 키는 요청 경로에서만 쓰여 더미로도 부팅. age 개인키를 GH Secrets에 넣지 않아 노출면 최소화(ADR-013 정합). 키 계약은 secrets.sh와 동일 유지 |
| 스모크에 NetworkPolicy 부정 테스트 포함 | 미지원 CNI에선 정책이 조용히 무시되므로(ADR-014) "차단이 실제로 되는가"를 능동 검증 — G9 회귀 게이트 |

### 검증 항목 (워크플로가 게이트하는 계약)

1. `kubectl apply -k`가 깨지지 않는다(매니페스트 정합).
2. 전 서비스 롤아웃 성공 + orderdb-init Job 완료(G10 부트스트랩 경로 — CI는 매번
   빈 클러스터라 **로컬에서 미실행이던 전체 부트스트랩 리허설을 매 실행 수행**).
3. ingress 단일 진입점: `/api/products` 200 + 시드 데이터 존재, `/internal` 404(ADR-012).
4. default-deny 차단 실효(ADR-014).

## 결과 및 트레이드오프

- **이미지 빌드가 러너에서 느리다**(2코어 풀빌드 ×4) — 총 소요 30분±. 트리거를
  좁혀 완화했지만 근본 해결은 E5(의존성 레이어 분리·캐시) + E4(GHCR push 후 CI는
  pull만). 그때 이 워크플로의 빌드 스텝을 GHCR pull로 교체한다.
- CI는 amd64로 빌드·검증한다 — 운영 arm64 이미지 검증은 E4(multi-arch) 소관.
- `overlays/ci`가 늘었지만 델타는 probe 예산 한 줄 — local과의 드리프트 위험 최소.
- 2단계 Argo CD 착수 조건: OKE 클러스터 + E4 완료. 그때 App-of-Apps vs 단일
  Application, ksops vs ESO(ADR-013 잔여 결정)를 함께 다룬다.
