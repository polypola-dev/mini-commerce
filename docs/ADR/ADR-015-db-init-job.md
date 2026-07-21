# ADR-015: orderdb 초기화 — k8s Job 이관 (initdb ConfigMap 대체)

- 상태: 승인 (2026-07-14)
- 관련: ADR-010(G4 — initdb ConfigMap 임시 승계의 원 결정), ADR-014(G9 — Job의
  postgres 접근 허용), ROADMAP G10, GH #9(k8s 이전 에픽), F3(Flyway — 스키마 소유권)

## 컨텍스트

orderdb "데이터베이스" 생성(compose의 `docker/postgres-init/01-create-orderdb.sql`
계약)을 G4에서 postgres initdb ConfigMap으로 임시 승계했다. initdb 방식의 한계:
**PGDATA가 비어 있는 최초 기동에만 실행**된다 — 스크립트 추가·변경이 기존 PVC에
반영되지 않고, 실행 여부가 "볼륨이 언제 만들어졌나"에 묶인다. G10은 이를 정식
메커니즘으로 이관한다.

시드 데이터는 대상이 아니다 — A6에서 Flyway `db/seed` local 프로파일 마이그레이션으로
대체 완료. 스키마도 대상이 아니다 — Flyway(order-api 단독) 소유(F3). 남은 것은
"orderdb 데이터베이스 존재 보장" 하나다.

## 결정

**local 오버레이 소유의 k8s Job(`overlays/local/db-init-job.yaml`)** 을 채택하고
G4의 initdb ConfigMap(`postgres-init`)은 제거한다. initContainer는 배제.

### 근거 (Job vs initContainer)

- **initContainer 배제**: order-api Deployment는 base(환경 무관) 소유인데 orderdb
  생성은 로컬 전용 관심사(운영은 Supabase, ADR-007) — base에 넣으면 소유권 위반,
  patch로 넣으면 앱 파드에 CREATE DATABASE급 자격증명이 들어간다. 파드 재시작마다
  실행되는 것도 불필요.
- **Job**: postgres/redis와 동일하게 local 오버레이가 단독 소유. initdb와 달리
  PGDATA 상태와 무관하게 재실행 가능(멱등 SQL — 존재 검사 후 생성). 자격증명은
  로컬 고정값(ADR-013과 동일 논리)이라 Job 스펙에 평문 수용.

### 동작 방식

- `pg_isready` 대기 루프(적용 순서 무의존) + `activeDeadlineSeconds: 300`으로
  실패 표면화. `restartPolicy: OnFailure` + `backoffLimit: 6`.
- **`ttlSecondsAfterFinished: 600`** — 완료 후 자동 삭제되고 다음
  `kubectl apply -k`가 재생성·재실행한다. 멱등이라 안전하고, "Job 스펙 변경 시
  기존 완료 Job과 immutable 충돌" 문제도 자연 회피(10분 내 재적용은 unchanged).
- 파드 라벨 `app.kubernetes.io/name: db-init` — G9 `allow-to-postgres`가 이 라벨을
  허용한다(default-deny 하에서 Job→postgres:5432 도달 경로).

## 검증 (2026-07-14)

- 기존 PVC(orderdb 존재): Job이 postgres 롤링 재시작을 대기 후 "no-op" 완료.
- 생성 분기: 동일 로직·이미지·자격증명·NetworkPolicy 경로로 미존재 DB(g10test)
  생성 성공 확인 후 정리. (실 PVC 삭제 후 전체 부트스트랩 리허설은 데이터 파괴라
  세션 정책상 미실행 — **다음 kind 클러스터 재생성 때 자연 검증**된다: README
  순서상 `apply -k`만으로 Job이 orderdb를 만든다.)
- initdb ConfigMap 제거 후 postgres 롤링: PVC 데이터 생존, `/api/products` 200.

## 결과 및 트레이드오프

- 신규 클러스터 기동 시 앱(order-api Flyway)이 Job보다 먼저 뜨면 orderdb 부재로
  크래시 재시작 후 복구된다 — F1(기동 순서 무의존)의 의도된 동작이며 initdb
  방식과 동일한 특성.
- compose 경로(`docker/postgres-init/`)는 그대로 둔다 — compose가 살아있는 동안
  같은 계약의 이중 표현(SQL 한 줄)은 수용, compose 폐기 시 함께 삭제.
- ttl 만료 후 `apply -k`마다 Job이 재실행된다(수 초, 멱등) — 소음이지만 무해.
  G11(Argo CD) 전환 시 Sync hook(PreSync) annotation 부여를 재평가.
