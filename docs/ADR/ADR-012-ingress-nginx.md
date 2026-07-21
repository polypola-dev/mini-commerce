# ADR-012: API 단일 진입점 — ingress-nginx 경로 라우팅

- 상태: 승인 (2026-07-14)
- 관련: ADR-008(kind — host 80/443 매핑·ingress-ready 라벨 선행), ADR-009(Helm은
  외부 컴포넌트 소비용), ROADMAP G6, GH #9(k8s 이전 에픽)

## 컨텍스트

지금까지 BFF(Next.js API Routes)는 백엔드 3개 서비스에 **포트별 직결**했다
(`API_BASE_URL`=18080, `ORDER_SERVICE_URL`=18081, `ORDER_ADMIN_SERVICE_URL`=18082 —
compose 포트 또는 port-forward). k8s에서는 경로 기반 단일 진입점으로 수렴한다.
컨트롤러는 ROADMAP에 ingress-nginx로 명시돼 있었고, G1(ADR-008)에서 이미 kind
클러스터에 host 80/443 → control-plane 매핑과 `ingress-ready` 라벨을 준비해뒀다.

## 결정

**ingress-nginx 4.15.1**(Helm, `ingress-nginx` 네임스페이스) + **base 소유 Ingress
리소스**(`k8s/base/ingress.yaml`)로 경로 라우팅한다.

### 라우팅 규칙 (최장 프리픽스 우선)

| 경로 | 백엔드 | 비고 |
|---|---|---|
| `/api/admin/orders` | order-admin:8082 | |
| `/api/orders` | order-api:8081 | |
| `/api` (그 외 전부) | shop-api:8080 | products/cart/reviews/notifications |
| `/internal/**` | **비노출** | 규칙 없음 → 404. 서비스간 전용(차단 강제는 G9) |

**주의**: `/api/admin/products`는 catalog admin(shop-api 소속)이라 `/api` 규칙으로
떨어진다 — admin 프리픽스가 전부 order-admin이 아니다. nginx의 최장 프리픽스
우선이 이 구분을 자연스럽게 처리한다.

### 배치 구조

- **컨트롤러**: Helm `ingress-nginx/ingress-nginx 4.15.1`, values는
  `k8s/ingress-nginx/values-local.yaml` 커밋(ADR-009 규약). kind에는 LoadBalancer가
  없어 **hostPort 노출**: 컨트롤러를 `ingress-ready` 라벨(control-plane)에 고정 +
  control-plane taint toleration + Service는 ClusterIP(pending 방지). 운영(OKE)은
  LoadBalancer 기반이라 `values-prod.yaml`로 분리 예정.
- **Ingress 리소스**: 라우팅 규칙은 환경 공통이라 **base 소유**. host 미지정
  (로컬 = `http://localhost` 직결), 운영 도메인·TLS는 prod 오버레이에서 patch.
- **BFF 계약**: 프론트 코드 변경 없음 — env var 3개를 전부 `http://localhost`로
  주면 단일 진입으로 수렴(브라우저 직접 호출분 `NEXT_PUBLIC_API_BASE_URL`은
  빌드타임 인라인이라 별도 명시 필요, k8s/README.md).
- **CORS**: 변경 없음 — 허용 origin은 여전히 `http://localhost:3000`(CM). 진입점이
  하나가 됐을 뿐 브라우저 origin 구도는 동일. 운영에서 프론트·API가 같은 도메인을
  쓰게 되면 그때 CORS 자체가 불필요해지는지 재검토.

## 검증 (2026-07-14, kind)

`http://localhost` 직접 호출, nginx access log의 upstream 포트로 백엔드 확정:
`/api/products`→8080(200), `/api/orders`→8081(403=무인증 도달), `/api/admin/orders`
→8082(403), `/api/admin/products`→8080(403, 최장 프리픽스 동작 확인),
`/internal/inventory`→404(비노출).

## 결과 및 트레이드오프

- 컨트롤러가 control-plane 단일 노드 고정(hostPort) — 로컬 한정 구조. 운영은
  LoadBalancer + replica로 달라지며 values 분리로 흡수.
- `/internal/**` 비노출은 "라우팅 규칙 부재"일 뿐 클러스터 내부에서는 여전히 열려
  있다 — 실제 차단은 G9(NetworkPolicy) 소관.
- rewrite 없이 경로를 그대로 전달하므로 백엔드 `@RequestMapping`과 Ingress 경로가
  1:1 — 새 최상위 경로 추가 시 `base/ingress.yaml` 규칙도 함께 갱신해야 한다.
