# ADR-010: 로컬 Postgres/Redis 배치 — 단순 StatefulSet + PVC 선택

- 상태: 승인 (2026-07-14)
- 관련: ADR-007(운영은 Supabase/Upstash 유지), ADR-009(Kustomize·Helm 역할 분담),
  ROADMAP G4, GH #9(k8s 이전 에픽)

## 컨텍스트

운영 DB/캐시는 Supabase·Upstash 유지로 이미 확정(ADR-007) — 클러스터에서 상태 저장
서비스를 직접 운영하지 않는다. 남은 결정은 **로컬(kind) 개발용 Postgres/Redis의 배치
방식**뿐이다. G3까지는 검증용 임시 Deployment + emptyDir(`k8s/dev/postgres-redis.yaml`,
휘발)로 때웠다.

후보: Bitnami Helm 차트 / 단순 StatefulSet + PVC 직접 작성.

## 결정

**단순 StatefulSet + PVC를 직접 작성**하고, `k8s/overlays/local/`이 소유한다
(`postgres.yaml`, `redis.yaml`). base에 두지 않는 이유: prod에는 이 리소스가 존재하지
않는다(Supabase/Upstash) — 환경 공통이 아닌 리소스는 해당 오버레이 소유가 ADR-009
구조상 정확하다.

### 근거 (vs Bitnami 차트)

| 기준 | StatefulSet+PVC 직접 작성 | Bitnami 차트 |
|---|---|---|
| 이미지 지속성 | 공식 `postgres:16-alpine`/`redis:7-alpine` | **2025-08 Broadcom 정책 변경** — 무료 `docker.io/bitnami` 카탈로그가 `bitnamilegacy`로 동결(업데이트 중단), 유료 Secure Images로 전환. 신규 채택 근거 상실 |
| 필요 기능 | 단일 인스턴스 + 영속화면 충분 | HA·복제·메트릭 등 차트 가치가 로컬 전용에선 0 |
| ADR-009 정합 | "직접 작성은 Kustomize" 원칙 그대로 | Helm은 외부 컴포넌트 소비용으로 남겨둠(G5 Strimzi, G6 ingress-nginx) |
| 학습 가치 | StatefulSet·PVC·StorageClass 직접 다룸 | values.yaml 간접층 뒤에 숨음 |
| compose 정합 | compose와 동일 공식 이미지 — 로컬 이중 환경 간 드리프트 없음 | 이미지·설정 체계가 compose와 달라짐 |

### 구현 세부

- **스토리지**: kind 기본 StorageClass `standard`(rancher local-path,
  WaitForFirstConsumer) 사용. `volumeClaimTemplates`로 postgres 1Gi / redis 512Mi.
  파드 재생성에도 데이터 유지, **클러스터 삭제 시엔 함께 소멸**(로컬 개발용으로 충분).
- **PGDATA**: 마운트 루트가 아닌 `/var/lib/postgresql/data/pgdata` 하위 디렉토리 지정 —
  볼륨 루트의 프로비저너 산출물과 initdb 충돌 방지 관례.
- **orderdb 생성**: G3의 `postgres-init` ConfigMap 계약 승계(initdb 스크립트는 PGDATA가
  빈 최초 기동에만 실행). Job/initContainer 정식 이관은 G10 소관.
- **Redis 영속화**: `--appendonly yes`(AOF) + `/data` PVC — 장바구니·ShedLock 상태가
  파드 재생성에도 유지.
- **Service**: 이름 `postgres`/`redis` 유지(앱 env 계약, F5). 단일 replica라 per-pod
  DNS가 불필요해 headless 서비스 없이 앱이 쓰는 ClusterIP 서비스를 governing service로
  지정.
- **오버레이 규약**: local kustomization에 base와 동일한 `namespace`·`part-of` 라벨
  트랜스포머 추가(오버레이 자체 리소스에도 적용되도록). local의 `OTEL_SDK_DISABLED`
  패치는 `kind: Deployment` 타깃이라 StatefulSet에는 영향 없음(의도된 동작).

## 검증 (2026-07-14, kind)

- PVC 2개 Bound(standard), postgres/redis Ready, 앱 4개 재기동 후 Flyway·시드 정상.
- **영속성 실증**: products 3행·orderdb flyway 이력·Redis 마커 기록 → `postgres-0`/
  `redis-0` 파드 삭제 → 재생성 후 전부 생존 확인. 앱 커넥션 풀 자동 복구(F1),
  readiness UP, `/api/products` 정상 응답.

## 결과 및 트레이드오프

- `k8s/dev/`에는 kafka.yaml만 남음 — G5(Strimzi vs 단일 StatefulSet)에서 대체 예정.
  **G5 주의사항 유지: Service 이름 `kafka:9092` 필수**(shop-api 기동 전제).
- local-path는 노드 로컬 디스크라 노드 간 이동 불가(RWO + 단일 replica라 실질 문제
  없음). 백업 없음 — 로컬 개발 데이터는 시드로 재생성 가능하므로 허용.
- prod 오버레이는 이 리소스를 모른다 — 운영 연결 정보는 Secret/ConfigMap으로 주입
  (F5 계약, G8에서 Secret 관리 방식 확정).
