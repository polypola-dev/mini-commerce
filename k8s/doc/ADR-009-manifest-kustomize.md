# ADR-009: 매니페스트 관리 — Kustomize base/overlays 선택

- 상태: 승인 (2026-07-13)
- 관련: ADR-007(운영 OKE), ADR-008(로컬 kind), ROADMAP G2, GH #9(k8s 이전 에픽)

## 컨텍스트

자작 백엔드 4개 서비스(shop-api/order-api/order-admin/order-batch)를 로컬(kind)과
운영(OKE) 두 환경에 배포해야 한다. 환경 간 차이는 이미지 출처(로컬 `kind load` vs
GHCR), 레플리카 수, Secret 주입 방식 정도로 예상되며, 앱 설정 자체는 F5 결정에 따라
**전부 환경변수로 흡수**되어 있다(k8s/prod 프로파일 없음, `backend/doc/CONFIGURATION.md`).

후보: Kustomize / Helm / 순수 YAML 복제.

## 결정

**Kustomize base/overlays 구조**를 사용한다. Helm은 자작 서비스에는 쓰지 않고,
**외부 컴포넌트 소비용**(ingress-nginx, Strimzi, 관측성 스택 등)으로만 쓴다.

```
k8s/
├── base/                  # 환경 무관 공통 매니페스트 (G3: Deployment/Service…)
│   ├── kustomization.yaml
│   └── namespace.yaml
└── overlays/
    ├── local/             # kind — 로컬 이미지, 최소 레플리카
    │   └── kustomization.yaml
    └── prod/              # OKE — GHCR arm64 이미지(E4), 운영 레플리카
        └── kustomization.yaml
```

### 근거 (vs 대안)

| 기준 | Kustomize | Helm | 순수 YAML×2 |
|---|---|---|---|
| 자작 서비스 4개 관리 | patch로 환경差만 표현 | values/template 간접층이 과함 | 중복 복제·드리프트 |
| 도구 설치 | **kubectl 내장**(v5.6.0) — `kubectl apply -k` | helm 별도 | 불필요 |
| 학습·가독성 | 순수 YAML 그대로 읽힘 | Go 템플릿 문법 학습 필요 | 읽기는 쉬우나 관리 불가 |
| GitOps(G11 Argo CD) | 네이티브 지원 | 네이티브 지원 | 가능 |

- 환경차가 이미 env var로 수렴돼 있어(F5) 오버레이가 얇다 — 템플릿 엔진(Helm)이
  해결할 만큼의 변주 자체가 없다. 배포 대상도 우리 자신뿐이라 차트 배포판이 불필요.
- Helm을 배제하는 게 아니라 역할을 나눈다: **직접 작성하는 것은 Kustomize,
  외부에서 가져오는 것은 Helm(또는 upstream 공식 매니페스트)**. G5 Strimzi,
  G6 ingress-nginx에서 적용.

### 함께 결정한 규약

- **네임스페이스**: 자작 워크로드는 단일 `mini-commerce` 네임스페이스. base가
  Namespace 리소스를 소유하고 `namespace:` 필드로 일괄 적용. 외부 컴포넌트
  (ingress-nginx, Strimzi 등)는 각자 upstream 기본 네임스페이스를 따른다.
- **공통 라벨**: `app.kubernetes.io/part-of: mini-commerce`를 base에서 일괄 부여
  (`labels` 트랜스포머, `includeSelectors: false` — selector 불변성 보장).
  서비스별 `app.kubernetes.io/name`은 G3에서 각 리소스에 부여.
- **환경 이름**: `local`(kind) / `prod`(OKE). 중간 환경(staging)은 필요해질 때 추가.
- **적용 명령**: `kubectl apply -k k8s/overlays/local` — 별도 kustomize 바이너리
  설치하지 않음(kubectl 내장 사용, CI에서도 동일).

## 결과 및 트레이드오프

- 오버레이가 두꺼워지면(환경별 patch 남발) 가독성이 급락한다 — patch는 이미지
  태그·레플리카·리소스량 수준으로 제한하고, 그 외 환경차는 계속 env var(ConfigMap/
  Secret)로 밀어낸다는 F5 원칙 유지.
- Helm 차트로 배포되는 외부 컴포넌트의 값 파일(values.yaml)은 `k8s/` 아래
  컴포넌트별 디렉토리에 함께 커밋해 재현성을 확보한다(G5/G6에서 구체화).
- Secret 실물은 오버레이에 두지 않는다 — G8(Secret 관리)에서 생성 스크립트/
  SealedSecrets 등 방식 결정 전까지 base/overlays에는 참조만 존재.
