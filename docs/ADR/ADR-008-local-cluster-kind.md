# ADR-008: 로컬 k8s 클러스터 — kind 선택

- 상태: 승인 (2026-07-13)
- 관련: ADR-007(운영 OKE Basic 확정), ROADMAP G1, GH #9(k8s 이전 에픽)

## 컨텍스트

운영 환경은 OCI Always Free 기반 OKE Basic(폴백 k3s)으로 확정됐다(ADR-007). G계열
매니페스트 작업(G2~G11)을 진행하려면 로컬 개발·검증용 클러스터가 필요하다.
후보는 kind / minikube / k3d.

제약·요구사항:

- 운영 OKE는 **멀티노드**(Ampere A1 arm64) — 로컬에서도 멀티노드 스케줄링,
  롤링 업데이트, PDB 동작을 재현할 수 있어야 함
- G11 1단계가 "GitHub Actions에서 kind 배포 검증" — CI에서 동일 도구 재사용
- 개발 머신은 Apple Silicon(arm64) — 운영과 아키텍처가 같아 이미지 arm64
  검증을 로컬에서 그대로 수행 가능해야 함
- G6에서 ingress-nginx를 붙일 수 있어야 함

## 결정

**kind**를 로컬 클러스터로 사용한다. 클러스터 정의는 `k8s/kind/cluster.yaml`
(control-plane 1 + worker 2, 이름 `mini-commerce`)로 리포에 커밋해 재현 가능하게 한다.

### 근거 (vs 대안)

| 기준 | kind | minikube | k3d |
|---|---|---|---|
| 멀티노드 | config 한 파일로 선언적 구성 | 지원하나 드라이버 의존·무거움 | 지원 |
| CI 재사용 | GitHub Actions 사실상 표준(G11 직결) | 가능하나 드묾 | 가능 |
| 배포판 | kubeadm 기반 **표준 k8s** — OKE와 동일 계열 | 표준 k8s | k3s(경량 변형) — 표준과 차이 존재 |
| arm64 | 네이티브 | 네이티브 | 네이티브 |
| ingress | extraPortMappings + ingress-ready 라벨 표준 레시피 | addon | 내장 traefik(제거 필요) |

- k3d는 운영 폴백(k3s)과 일치한다는 장점이 있지만, 1순위 운영 타깃이 OKE(표준
  k8s)이므로 표준 배포판인 kind가 우선. k3s 폴백이 현실화되면 그때 재평가.
- 로컬 클러스터 정의를 CI(G11)에서 그대로 재사용하는 것이 kind의 결정적 이점.

### 클러스터 구성 결정 사항

- **control-plane 1 + worker 2**: 멀티노드 스케줄링 재현이 kind 선택의 핵심 근거이므로
  단일 노드로 만들지 않는다. 스모크 테스트로 워커 2대 분산 스케줄링 확인 완료.
- **control-plane에 `ingress-ready=true` 라벨 + 80/443 hostPort 매핑**: G6
  ingress-nginx kind 표준 레시피 선행 준비. 호스트 80/443 비어 있음 확인.
- k8s 버전은 kind 기본 노드 이미지 추종(현재 v1.34.0). OKE 구축 시 버전 차이 확인.

## 결과 및 트레이드오프

- 로컬 이미지는 레지스트리 없이 `kind load docker-image`로 주입 — 클러스터 재생성
  시 재주입 필요. E4(GHCR 파이프라인) 이후에는 pull 방식과 병행 가능.
- kind는 LoadBalancer 타입 미지원 — Service는 ClusterIP/NodePort, 외부 노출은
  G6 Ingress로 통일 (cloud-provider-kind는 도입하지 않음).
- Docker Desktop VM 메모리(현재 8GB)를 compose 스택과 공유 — G3에서 4개 서비스를
  클러스터에 올려 검증할 때는 **docker compose 스택을 내리고 진행**하거나 VM
  메모리 증설 필요(서비스당 limit 1Gi × 4 + 인프라).
- 클러스터 수명주기: `kind create/delete cluster --config k8s/kind/cluster.yaml`,
  kubectl context는 `kind-mini-commerce`.
