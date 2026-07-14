# ADR-014: NetworkPolicy — default-deny(Ingress) + env var 계약 기반 명시 허용

- 상태: 승인 (2026-07-14)
- 관련: ADR-012(ingress-nginx — /internal 비노출, 차단 강제는 본 ADR), ADR-011(Kafka
  listener 무인증의 전제), ROADMAP G9, GH #9(k8s 이전 에픽), B3(/internal 서비스간 인증),
  backend/doc/CONFIGURATION.md(env var 계약)

## 컨텍스트

`/internal/**`(서비스간 전용 API)은 Ingress 라우팅에서 의도적으로 제외했지만(ADR-012),
그것은 "외부에서 경로가 없다"일 뿐 네트워크 차단이 아니다. 클러스터 안에 뜬 임의
파드는 여전히 `shop-api:8080/internal/...`을 직접 때릴 수 있다. Kafka listener도
무인증 plaintext(ADR-011)라 네임스페이스 내 도달 제어가 전제였다. B3(서비스간 인증,
앱 레이어)의 인프라 레이어로서 L3/L4 차단이 필요하다.

## 결정

**`mini-commerce` 네임스페이스에 default-deny(Ingress) 1개 + 파드별 명시 허용
NetworkPolicy를 둔다.** 허용 규칙은 각 서비스의 env var 계약(CONFIGURATION.md)과
1:1로 대응시키고, 계약에 없는 경로는 열지 않는다.

### 트래픽 매트릭스 (허용 = 계약 근거)

| 대상 | 포트 | 허용 소스 | 근거 |
|---|---|---|---|
| shop-api | 8080 | ingress-nginx 컨트롤러 | G6 단일 진입점 |
| shop-api | 8080 | order-api·order-admin·order-batch | CATALOG_BASE_URL (admin/batch는 현재 미호출이나 order-infra 전이의존으로 클라이언트 조립되는 계약 유지) |
| order-api | 8081 | ingress-nginx 컨트롤러, shop-api | INVENTORY_BASE_URL |
| order-admin | 8082 | ingress-nginx 컨트롤러만 | 서비스간 인바운드 계약 없음 |
| order-batch | — | 없음 (default-deny만) | Service 자체가 없음 |
| postgres | 5432 | 앱 4종 (local 오버레이 소유) | DATABASE_URL |
| redis | 6379 | 앱 4종 (local 오버레이 소유) | REDIS_HOST |
| kafka | 9090/9091/9092… | **Strimzi operator 자동 생성 정책이 소유** | NetworkPolicy는 추가 허용(additive)이라 default-deny와 공존 |

### 소유권 배치 (ADR-009 연장)

- `base/network-policy.yaml` — default-deny + 앱 4종 허용 (환경 무관: prod도 동일 구도)
- `overlays/local/network-policy.yaml` — postgres/redis 허용 (운영은 Supabase/Upstash로
  클러스터 외부라 local만 소유, G4와 동일 논리)

### Egress는 제한하지 않는다 (Ingress-only)

- 위협 모델이 "무엇이 앱 파드에 **들어올** 수 있나"(= /internal·Kafka·DB 도달 제어)이고,
  그 목적은 Ingress 차단만으로 달성된다.
- Egress default-deny는 DNS(kube-system), Supabase/JWKS·Upstash(외부 IP 고정 불가),
  Kafka·서비스간 호출 전부를 재열거해야 해 취약(brittle)한 반면, 솔로 프로젝트에서
  막는 실질 위협(내부 파드의 임의 유출)이 현 단계 리스크 대비 비용이 크다.
  운영 보안 강화 단계에서 재평가.

## 세부 결정·주의점

- **ingress-nginx 소스 식별**: `namespaceSelector(kubernetes.io/metadata.name=ingress-nginx)`
  + `podSelector(app.kubernetes.io/name=ingress-nginx, component=controller)`.
  자동 부여 라벨이라 Helm 재설치에도 안정. 컨트롤러는 hostNetwork가 아니어서(파드 IP
  소스) namespaceSelector 매칭이 성립한다 — hostNetwork로 바꾸면 이 정책이 깨진다.
- **kubelet probe**: NetworkPolicy 적용 대상이 아니다(노드 발신) — default-deny에도
  startup/liveness/readiness 전부 생존. order-batch가 그 증빙(허용 0개인데 Ready).
- **`kubectl port-forward`는 뚫린다**: kubelet 경유(파드 loopback)라 정책 미적용.
  디버깅 편의는 유지되고, 이는 kubectl 권한 있는 사람 = 이미 클러스터 관리자라 수용.
- **L7 구분은 하지 않는다**: ingress-nginx가 도달 가능한 8080에는 /internal도 함께
  열려 있으나, nginx에는 /internal 라우팅 규칙이 없어(ADR-012) 외부 경로가 없다.
  경로 위장·신원 검증은 B3(앱 레이어 인증) 소관 — 본 ADR은 네트워크 레이어만 담당.
- **kind 환경 전제**: kind v0.30 kindnetd는 NetworkPolicy 네이티브 지원(구버전은
  미지원 CNI였음). OKE(운영)는 표준 CNI(OCI VCN-Native/flannel+Calico 여부는 G11
  시점 확인)에서 동일 매니페스트로 동작 — 미지원 CNI면 정책이 **조용히 무시**되므로
  운영 전환 시 부정 테스트(차단 확인) 재실행이 필수.

## 검증 (2026-07-14)

`kubectl apply -k k8s/overlays/local` 후:

- 전 파드 Ready 유지(probe 생존), `http://localhost/api/**` 경유 4서비스 정상.
- 긍정: shop-api 라벨 파드 → order-api:8081·postgres:5432·redis:6379·kafka:9092 연결 성공.
- 부정: 무라벨 파드(동일 ns) → shop-api:8080 차단, shop-api 라벨 → order-admin:8082 차단,
  타 ns(default) 파드 → shop-api:8080 차단 확인.

## 결과 및 트레이드오프

- 새 서비스/스크래퍼(H계열 Prometheus 등)가 앱 파드에 접근하려면 **허용 정책 추가가
  선행**되어야 한다 — default-deny라 "안 열면 안 된다"가 기본값. H1(관측성) 때
  metrics 포트 허용 정책이 필요할 것.
- order-admin/order-batch → shop-api 허용은 "미호출이지만 계약 유지"라 최소권한
  관점에선 과허용 — env var 계약을 정리(B계열)하면 함께 닫는다.
- Kafka 정책을 Strimzi에 위임한 대가로 9092가 네임스페이스 내 전 파드에 열려
  있다(무인증, ADR-011). 조이려면 Kafka CR `networkPolicyPeers`로 앱 4종만 허용
  가능 — 운영 전환 시 함께 결정.
