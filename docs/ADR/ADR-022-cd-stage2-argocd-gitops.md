# ADR-022: CD 2단계 — Argo CD GitOps (App-of-Apps + ksops)

- 상태: 제안 (2026-07-23) — OKE 클러스터 확보 후 실전 검증하며 확정
- 관련: ROADMAP G11(2단계), GH #9(k8s 이전 에픽), ADR-007(OKE),
  ADR-009(Kustomize), ADR-011(Strimzi), ADR-012(ingress-nginx),
  ADR-013(Secret SOPS/age — 잔여 결정을 여기서 종결), ADR-016(CD 1단계),
  ADR-017(GHCR arm64 이미지)

## 컨텍스트

CD 1단계(ADR-016)는 GitHub Actions에서 kind에 push 배포하는 **검증 게이트**였다 —
운영 클러스터가 없어 pull 기반 GitOps는 유보했다. 이제 OKE 구축 + E4(GHCR arm64)
완료로 전제가 갖춰져, 2단계에 착수한다: **클러스터 안의 Argo CD가 Git을 진실의
원천으로 삼아 지속 동기화**한다.

세 갈래 결정이 얽혀 있다: (1) Application 구성 패턴, (2) Secret을 Argo가 소비하는
방식(ADR-013이 미룬 ksops vs External Secrets), (3) 이미지 태그 갱신 흐름.

## 결정

### 1. App-of-Apps 패턴 (vs 단일 Application, ApplicationSet)

**App-of-Apps 채택.** 루트 Application(`bootstrap`) 하나가 `k8s/argocd/apps/`의
child Application들을 관리한다. child는 이질적(Helm 2 + kustomize 2)이고 배포 순서
의존이 있어 **sync-wave로 순서를 선언**한다:

| wave | 컴포넌트 | 소스 |
|---|---|---|
| 0 | Strimzi operator, ingress-nginx | Helm (multi-source로 커밋된 values 참조) |
| 1 | Kafka 클러스터 CR | kustomize (`k8s/kafka`, CR만 include) |
| 2 | 앱 5종 + Ingress + NetworkPolicy + Secret | kustomize (`k8s/overlays/prod`) |

- **ApplicationSet 배제**: 동종 대량 템플릿(멀티클러스터·PR 프리뷰)용인데 우리는
  이질적 소수 컴포넌트라 명시적 App-of-Apps가 더 읽힌다. 멀티환경으로 커지면 재평가.
- **단일 Application 배제**: Helm/kustomize 혼재와 wave 게이트를 한 Application에
  담을 수 없다.
- **wave 1→2 게이트의 실효성**: Argo 기본엔 `kafka.strimzi.io/Kafka` health 판정이
  없어 CR 적용 즉시 Healthy로 오판 → 앱이 브로커 준비 전 부팅 → bootstrap DNS
  크래시(G3). `install/values.yaml`에 Kafka Ready condition을 읽는 커스텀 health
  check(Lua)를 등록해 **wave 2가 Kafka Ready를 실제로 기다리게** 한다.

### 2. Secret — ksops (CMP) 채택, External Secrets/OCI Vault 배제

ADR-013이 "G11에서 재평가"로 남긴 결정을 **ksops로 종결**한다. Argo repo-server에
**init container**로 ksops 내장 kustomize를 설치하고(정본 viaduct-ai/kustomize-sops
권장 방식 — CMP 사이드카보다 단순, kind 리허설에서 확정), `overlays/prod`의 KSOPS
generator가 참조하는 `k8s/secrets/prod-app-secrets.enc.yaml`을 매니페스트 생성 시점에
복호화한다. `kustomize.buildOptions`에 `--enable-alpha-plugins --enable-exec`를 주고,
age 개인키는 `sops-age` Secret으로 repo-server에 주입한다.

**근거 (vs External Secrets Operator + OCI Vault):**
- **연속성**: ADR-013의 SOPS/age 워크플로(.env → seal → enc 커밋)를 그대로 소비 —
  `.sops.yaml`·`secrets.sh`·기존 체계 재사용. 로컬(kind)과 운영이 같은 도구.
- **$0**: OCI Vault 의존·비용·IAM(instance principal) 설정이 없다. ADR-007 목표 정합.
- **솔로·단일 키**: ESO의 감사/로테이션/외부 스토어 이점이 1인 운영에선 과投資.
- **트레이드오프(수용)**: age 개인키가 클러스터(repo-server) 안에 산다 — 단, 키
  1개·한 곳이라 폭발 반경이 좁다. ksops 이미지가 distroless라 init 명령을 셸 없이
  직접 호출해야 하는 점이 유일한 함정(리허설에서 확인).
- **재평가 트리거**: 협업자 합류·비밀 로테이션 요구·감사 필요 시 ESO+OCI Vault로 이행.
  그때도 enc 커밋 대신 Vault를 원천으로 바꾸는 국소 변경.
- **운영 키 분리 권장**: 로컬 kind용 age 키와 별개의 운영 전용 키로 봉인 →
  로컬 키 유출이 운영 비밀로 번지지 않게.

### 3. 이미지 태그 갱신 — Git write-back (Argo Image Updater는 유보)

운영 이미지는 `overlays/prod`에 **불변 태그(semver 또는 sha, E4/ADR-017 산출)**로 pin.
갱신은 **Git 커밋을 원천으로 유지** — CI(build-push) 후 태그를 오버레이에 커밋(수동
PR 또는 CI write-back)하면 Argo가 동기화. Argo Image Updater(레지스트리 폴링 자동
bump)는 편하지만 Git 우회 write-back이라 "Git=진실" 원칙을 흐린다 — 운영 초기엔
배제하고, 배포 빈도가 오르면 write-back 모드로 재평가.

### sync 정책

앱·인프라 모두 `automated: {prune, selfHeal}` + `ServerSideApply=true`(대형 CRD의
last-applied 애노테이션 초과 회피). 운영 초기 안정화 기간엔 selfHeal을 잠시 꺼
수동 sync로 관찰하는 것도 허용(README).

## 범위 밖 (명시)

- **관측성(H계열)**: kube-prometheus-stack을 App-of-Apps에 편입할지는 OKE 리소스
  (4 OCPU/24GB 공유) 여유와 Grafana Cloud 대안 결정에 달려 stage 2 초기 제외.
  편입 시 `apps/30-observability.yaml`(wave 3).
- **Argo CD 자체의 GitOps 관리**: 부트스트랩 순환을 피해 Argo는 Helm으로 설치·관리
  (self-management는 안정화 후 선택).

## 검증 1부 — kind 리허설 (2026-07-23, 완료)

기존 kind 클러스터에 Argo CD(차트 10.1.4, app v3.4.5)를 설치해 **신규 기계장치**를
OKE 없이 선실증했다(매니페스트는 클러스터 무관 재사용):

- ✅ ksops 복호화 end-to-end: repo-server에서 `kustomize build`가 실제 enc 파일을
  복호화해 올바른 평문 Secret 생성(secrets.sh 산출과 값 일치).
- ✅ init container가 ksops 내장 kustomize(`v5.3.0+ksops.v4.5.1`) 설치, age 키 마운트 확인.
- ✅ Kafka health check(Lua)·`kustomize.buildOptions`가 argocd-cm에 로드됨.
- ✅ Application 6종 server-side dry-run 통과(실제 Argo CRD 스키마 검증).
- 🐛 잡은 버그: ksops 이미지 distroless라 `/bin/sh -c` init 방식 CrashLoop →
  정본 `ksops install --with-kustomize` 직접 호출로 수정. install/values.yaml 반영.

## 검증 2부 — OKE 실전 (클러스터 확보 후)

1. prod 오버레이를 대상 context로 `kubectl kustomize` dry-run(KSOPS generator 포함).
2. OKE 부트스트랩(README 순서) → 전 wave Healthy(**wave 1→2 Kafka 게이트 실전 확인**)
   → `/api` 200 + `/internal` 404 + G9 차단 회귀(ADR-014, OKE CNI에서 재실행 필수).
3. drift 테스트: 수동 `kubectl edit` 후 selfHeal 복원 확인.

## 결과 및 트레이드오프

- push(CI가 클러스터 자격증명 보유) → pull(Argo가 Git 당김)로 전환 — CI에서 kubeconfig
  제거, 클러스터 자격증명 노출면 축소.
- 배포 이력 = Git 커밋 이력, 롤백 = `git revert`.
- 대가: Argo CD 자체가 OKE 리소스를 점유(4 OCPU/24GB 예산에서 무시 못 함) — 컴포넌트
  수·복제본을 최소로 설치. ksops CMP 사이드카 설정이 초기 러닝코스트.
- 현재 산출물은 **스캐폴드** — OKE 미확보라 values-prod.yaml, prod 오버레이 실체,
  prod enc Secret, 버전 pin은 TODO(README·각 파일 주석에 표시).
