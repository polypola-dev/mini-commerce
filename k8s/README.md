# k8s — 로컬(kind) 및 매니페스트

k8s 전환(ROADMAP G계열, GH #9 에픽) 산출물 디렉토리.

## 구성

- `kind/cluster.yaml` — 로컬 kind 클러스터 정의 (G1, ADR-008)
- `doc/` — k8s 관련 ADR·문서
- (예정) Kustomize base/overlays — G2에서 구조 결정

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
