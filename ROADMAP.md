# mini-commerce 로드맵 — 필수 기능 · 버그 · k8s 전환 (2026-07-05)

> **[2026-07-06 결정] I2 확정 — 운영을 Oracle Cloud Always Free 기반 k8s(OKE, 폴백 k3s)로 이전 (ADR-007).**
> DB Supabase·Redis Upstash·프론트+BFF Vercel 유지, Render는 이전 완료 후 폐기.
> ARM(arm64) 이미지 필수 → E4/E5에 multi-arch 요구 상향. G계열은 학습이 아닌 운영 목표.
>
> 저장소·Obsidian 위키·GitHub 이슈(#3~#5)·Supabase advisor·docker-compose 분석 결과를 종합한 백로그.
> 우선순위: **P0** = 해당 트랙에서 최우선(서비스 트랙이면 즉시 해결, k8s 트랙이면 착수 첫 단계),
> **P1** = 다음 마일스톤, **P2** = 학습/개선 목표.
>
> 현재 구성: shop-api(catalog/cart/review/notification/BFF) + order-api(order/inventory) +
> order-admin + order-batch, Kafka(KRaft 단일 브로커), Postgres(minicommerce/orderdb),
> Redis, Tempo/Loki/Prometheus/Grafana — 전부 로컬 docker-compose 기반.

---

## A. 버그 및 알려진 결함 (7)

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| A1 | P1 | 검색 오버레이 "추천 검색어" 칩(입력창만 채움)과 홈/카테고리 칩(`/search?q=` 풀페이지 이동) 동작 불일치 해소 | HANDOFF 세션 15 잔여 |
| A2 | P1 | 장바구니 상품 이미지 N+1 조회 제거 — 매 로드마다 productId별 `getProductById` 병렬 호출 → `CartItem`에 imageUrl 저장(백엔드) | 장바구니 커지면 성능 저하 |
| A3 | P1 | `next lint` `Invalid project directory` 오류 원인 조사·수정 | 세션 8부터 보류, 린트가 사실상 비활성 상태 |
| A4 | P2 | 검색을 Intercepting Routes(`@modal/(.)search`)로 재전환 — 상품상세는 동일 패턴 정상 동작 확인됨 | 세션 11 실패 원인 미규명 |
| A5 | P1 | `frontend` cart-drawer 기존 테스트 타입에러 1건 수정 — `tsc --noEmit` 완전 그린화 | 세션 13에서 "무관한 기존 에러"로 방치됨 |
| A6 | P0 | ✅ **완료(2026-07-13, GH #14)** — shop-api SeedData(ApplicationRunner)가 order-api에 동기 REST 호출하던 구조 제거. 상품 시드는 `db/seed/` Flyway 마이그레이션(local profile 전용)으로 대체, 재고 REST 호출은 완전 삭제(어드민 API로 대체). compose `depends_on`에서 order-api 제거 | 기존 `depends_on: service_healthy` 봉합 해소 |
| A7 | P2 | `/maintenance` 점검중 페이지가 실제 점검모드 토글과 미연동 — feature flag 연결 | CONTEXT.md 명시 |

## B. 보안 (8)

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| B1 | P0 | Supabase `public` 스키마 11개 테이블 **RLS 정책 설계 후 활성화** | Supabase advisor critical, TASKS.md 대기 중 |
| B2 | P0 | `SECURITY DEFINER` 함수 노출 점검·제거 | Supabase advisor 지적 (obs 601) |
| B3 | P0 | `/internal/**` 서비스간 API 무인증 상태 해소 — 내부 토큰/mTLS, k8s 전환 시 NetworkPolicy 병행 | order-api 헬스체크가 "인증 안 걸림"을 활용 중일 정도로 완전 개방 |
| B4 | P1 | docker-compose 하드코딩 크리덴셜(`minicommerce/minicommerce`) 및 시크릿 주입 방식 정리 — `.env` 표준화 → k8s Secret으로 승계 | BFF_SECRET_KEY, SERVICE_ROLE_KEY 포함 |
| B5 | P1 | API rate limiting — 로그인/주문/리뷰 엔드포인트 브루트포스·도배 방어 (Redis 기반) | 현재 전무 |
| B6 | P1 | 의존성 취약점 스캔 자동화 — Dependabot + 이미지 Trivy 스캔 | CI 자체가 없음 (E1 선행) |
| B7 | P2 | ADR-003 dedup 레이어(다중계정 어뷰징 방지) 구현 — 쿠폰/포인트 기능(C6) 착수 전 필수 선행 | ADR proposed 상태 |
| B8 | P1 | 브라우저→백엔드 직접 호출 경로(BFF 터널 우회) 정리 — `NEXT_PUBLIC_API_BASE_URL`로 reviews/상품상세를 백엔드에 직접 fetch하는 경로가 존재해 백엔드 공개 노출 + CORS가 강제됨. BFF 경유로 통일 가능한지 검토(불가하면 공개 read-only 경로를 명시적으로 분리) | `lib/api.ts:164,295` 확인. G6 Ingress 라우팅 설계에도 직결 |

## C. 필수 기능 (커머스 코어) (10)

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| C1 | P0 | **실 PG 연동** (토스페이먼츠 등 샌드박스) — 현재 결제는 `CompletePaymentUseCase` mock 완료 처리 | 정식 서비스 표방과 최대 갭 |
| C2 | P0 | 결제 후 주문취소 — PG 환불 연동 + 확정재고 재입고 (**GH #4**) | C1과 한 몸. 보상 트랜잭션 설계 |
| C3 | P0 | 배송지 백엔드 저장 — 현재 localStorage(`lib/addresses.ts`)라 기기 간 미동기화·유실 | 주문 도메인엔 배송지가 있는데 주소록만 로컬 |
| C4 | P1 | 위시리스트 백엔드 저장 — 현재 localStorage(`lib/wishlist.ts`) | C3과 같은 패턴, 같이 처리 |
| C5 | P1 | 상품 카테고리 도메인 모델 도입 — 현재 `/category`는 검색 키워드 필터로 흉내 | catalog에 분류 체계 없음 |
| C6 | P2 | 쿠폰/포인트/혜택 도메인 — 화면 기획 포함 | B7(dedup) 선행 필수 |
| C7 | P1 | 알림 실채널 발송 — notification 도메인을 이메일(Resend 등)/웹푸시로 연결 | 현재 인앱 목록성 |
| C8 | P1 | 배송 상태 세분화 + 배송 추적 화면 — 주문 상태 머신에 배송 단계 반영 | 관리자 상태 변경만 존재 |
| C9 | P2 | 리뷰 수정 기능 + 이미지 첨부 — 현재 작성/삭제만 | 이미지 스토리지(Supabase Storage) 검토 |
| C10 | P2 | 회원 탈퇴 및 개인정보 삭제 플로우 — Supabase user 삭제 + 주문 데이터 익명화 정책 | 정식 서비스 컴플라이언스 |

## D. 아키텍처 / 기술부채 (9)

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| D1 | P0 | **Flyway 도입, `ddl-auto: update` 제거** — shop-api/order-api가 update로 운영 중, 마이그레이션 도구 부재. `docker/postgres-init/*.sql`도 Flyway로 흡수 | k8s에서 다중 레플리카 동시 DDL은 사고 지점. F계열 전체의 선행 조건 |
| D2 | P1 | OrderPlacedEvent/OrderPaidEvent 계약 모듈 추출 `order:order-events` (**GH #5**) | 발행자/구독자 클래스 중복 제거 |
| D3 | P2 | inventory 별도 서비스+DB 추출 — 분산 사가 학습 에픽 (**GH #3**, 전략 c) | k8s 전환 후 진행 권장 |
| D4 | P1 | Kafka consumer 재시도/DLQ 정책 수립 — 역직렬화 실패·처리 실패 시 유실 방지 | serializer 사고(a05581f) 전력 있음 |
| D5 | P2 | 이벤트 스키마 관리 — JSON 유지 vs Avro/Protobuf+Schema Registry 결정 (ADR로) | D2 이후 |
| D6 | P1 | Catalog↔Order 양방향 동기 REST 의존 완화 — 재고조회(INVENTORY_BASE_URL)·상품조회(CATALOG_BASE_URL) 상호 호출 구조에 서킷브레이커(Resilience4j)+타임아웃 도입 | obs 595 트레이드오프 문서화됨. 한쪽 다운 시 연쇄 장애 |
| D7 | P1 | ArchUnit 헥사고날 의존성 검증 부활 — 모듈 경계·domain 순수성 자동 검증 | 2026-06-19 삭제됐으나 멀티모듈 완성 후 재도입 시점 도래 |
| D8 | P2 | cart/review/notification 레거시 플랫 → 헥사고날 전환 (안전망 테스트 선행) | ARCHITECTURE.md ⚠️ 미전환 |
| D9 | P2 | catalog/inventory 모듈 내부 구조 헥사고날 전환 | 모듈 분리만 완료, 내부는 레거시 |

## E. 테스트 / CI·CD (6)

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| E1 | P0 | **GitHub Actions CI 구축** — 백엔드 `gradlew build`(test 포함) + 프론트 `tsc`/`next build`, PR 게이트 | 현재 워크플로 0개. 모든 자동화의 기반 |
| E2 | P1 | Testcontainers 통합 테스트 — Postgres/Redis/Kafka 실컨테이너 기반, CI에서 실행 | Redis 동시성 테스트는 있으나 로컬 인프라 의존 |
| E3 | P1 | 프론트 E2E 스모크 (Playwright) — 로그인→상품→장바구니→주문 핵심 여정 | 세션마다 수동 클릭 검증에 의존 중 |
| E4 | P1 | Docker 이미지 빌드·푸시 파이프라인 — GHCR, 태그 전략(sha/semver), **arm64 필수**(운영이 OCI Ampere — ADR-007). GHA amd64 러너면 buildx/QEMU 또는 ARM 러너 | k8s 배포(G계열)의 전제 |
| E5 | P1 | Dockerfile 최적화 — `COPY . .` 전체 복사로 레이어 캐시 전멸 → 의존성 선복사, Gradle 캐시 마운트, **multi-arch(amd64+arm64) 필수화**(ADR-007) | 빌드 시간·CI 비용 직결 |
| E6 | P2 | k6 부하 테스트 — 재고 동시성(reserve/confirm/release) 시나리오, HPA 튜닝(G7) 근거 데이터 | MSA 분리 검증 겸용 |

## F. k8s 전환 — 애플리케이션 선행 작업 (7)

> compose의 `depends_on`/헬스체크로 봉합한 것들은 k8s에서 전부 깨진다. 매니페스트 작성 전에 앱부터 k8s-ready로.

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| F1 | P0 | ✅ **완료(2026-07-13, GH #14)** — 기동 순서 의존 완전 제거. order-admin/order-batch → order-api 앱간 `depends_on` 제거(DDL 경합 없음이 확인돼 근거 자체가 무효), 두 서비스에 Hikari `initialization-fail-timeout: -1` 추가. Kafka/Redis는 4개 서비스 전부 lazy 연결로 이미 안전함을 코드 조사로 확인. docker-compose에서 postgres를 껐다 켜며 order-admin/order-batch가 부팅 성공 후 자동 복구되는 걸 실증(e2e) | shop-api/order-api의 Postgres 의존만 Flyway 소유권에 따른 정당한 hard dependency로 유지 |
| F2 | P0 | ✅ **완료(2026-07-13, GH #15)** — 4개 모듈 전부 `management.endpoint.health.probes.enabled: true` + health 노출로 `/actuator/health/liveness`·`/readiness` 분리 활성화(compose는 k8s가 아니라 자동 활성화 안 됨 → 명시). readiness는 기본 `readinessState`만 포함(DB/Redis/Kafka 미포함) — 외부 의존 순단 시 전 레플리카 동시 이탈(연쇄 장애) 방지, F1 독립 부팅 원칙의 연장. order-api의 `/internal/inventory/stocks` 편법 healthcheck 제거, compose healthcheck 4개 전부 `/actuator/health/readiness`로 교체(shop-api는 신규 추가). 실컨테이너 재빌드 후 4개 모듈 probe 200 UP + 전 서비스 healthy 수렴 검증 | 인증 필터가 URL 패턴 화이트리스트 방식(`/api/**`만 등록)이라 `/actuator/**`는 무인증 — B3 도입 시에도 probe 경로 분리 요건 자동 충족 |
| F3 | P0 | ✅ **완료(2026-07-13, GH #11에서 해소 확인)** — ddl-auto 스키마 생성 구조는 Flyway 도입(GH #11)으로 이미 대체됨: 4개 모듈 전부 `ddl-auto: validate`, orderdb 마이그레이션은 order-api 단독 소유(admin/batch는 flyway disabled + validate-only). 잔여분은 `CREATE DATABASE orderdb`(docker/postgres-init)의 k8s Job/initContainer 이관뿐인데 이는 **G10 소관** — 앱 레벨 F3는 종결 | Flyway 앱 내 실행 vs 별도 Job 분리는 G10에서 결정 |
| F4 | P1 | ✅ **완료(2026-07-13)** — 4개 모듈 `server.shutdown: graceful` + `lifecycle.timeout-per-shutdown-phase: 30s` 명시(Boot 3.4+ 기본값이지만 컨테이너 유예와의 정렬 계약이라 코드에 고정), order-batch는 `task.scheduling.shutdown.await-termination: true/25s`로 스케줄 작업(리퍼/스윕) 완료 대기. compose `stop_grace_period: 35s`(기본 10s는 graceful을 SIGKILL로 무효화), Dockerfile은 `sh -c "exec java …"`로 SIGTERM이 PID 1 java에 전달됨을 보장. `docker compose stop`으로 "Commencing graceful shutdown → complete" 실증. k8s preStop/terminationGracePeriodSeconds(40s) 값은 CONFIGURATION.md에 정의, 적용은 G3 | 시간 사슬: 앱 유예 30s < compose 35s < k8s 40s |
| F5 | P1 | ✅ **완료(2026-07-13)** — `backend/doc/CONFIGURATION.md` 작성: 서비스별 환경변수 계약 표 + ConfigMap/Secret 분류, graceful shutdown 시간 사슬, 리소스 산정표(G3 입력값). 프로파일 전략 확정: **`k8s`/`prod` 프로파일은 만들지 않음** — 환경차는 전부 env var로 흡수(12-factor), `local`은 환경 분기가 아닌 시드 on/off 스위치로만 유지 | compose environment 블록 = 계약 원본, 문서는 k8s 매핑 |
| F6 | P1 | ✅ **완료(2026-07-13)** — Dockerfile `ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"`(env로 통째 교체 가능) + compose `mem_limit: 1g`로 k8s limits 예행. 실증: 힙 최대 742M(1GiB×75%) 컨테이너 인지 확인, RSS 무제한 777~983Mi → limit 하 496~549Mi 수렴. requests/limits 산정표(memory request=limit 1Gi, **CPU limit 없음** — 부팅 JIT burst 스로틀 방지, request만 100~250m)는 CONFIGURATION.md | 절대값 -Xmx 대신 비율 — limit 조정 시 재빌드 불필요 |
| F7 | P2 | order-batch 멀티 레플리카 최종 검증 — ShedLock(@SchedulerLock) + `app.batch.enabled` 플래그 게이팅이 이미 견고하게 구현돼 있고 2 replica 기동도 확인됨(obs 839). 남은 것은 k8s Deployment replicas>1 + 롤링 업데이트 중 락 동작 확인뿐 | 코드 재검증 결과 사실상 완료 — 우선순위 하향 |

## G. k8s 인프라 구축 (11)

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| G1 | P0 | ✅ **완료(2026-07-13)** — 로컬 클러스터 **kind 확정(ADR-008, `k8s/doc/`)**: 표준 k8s(kubeadm)로 OKE와 동일 계열, GitHub Actions 재사용(G11 직결), arm64 네이티브. `k8s/kind/cluster.yaml`(control-plane 1 + worker 2, ingress-ready 라벨 + 80/443 매핑) 커밋, v1.34.0 클러스터 기동·워커 분산 스케줄링 스모크 검증 완료. **운영 클러스터는 OKE Basic(폴백 k3s)로 확정(ADR-007)** | G3 검증 시 compose 스택과 Docker VM 메모리(8G) 공유 주의 |
| G2 | P0 | ✅ **완료(2026-07-13)** — **Kustomize base/overlays 확정(ADR-009, `k8s/doc/`)**: `k8s/base` + `overlays/{local,prod}` 골격 커밋, kubectl 내장 kustomize 사용(`kubectl apply -k`, 별도 설치 없음). 네임스페이스 `mini-commerce` 단일, 공통 라벨 `part-of` base 일괄 부여, Helm은 외부 컴포넌트(G5 Strimzi·G6 ingress-nginx) 소비용으로 역할 분리. local 오버레이 kind 적용 검증 완료 | patch는 이미지·레플리카·리소스량으로 제한, 환경차는 env var 유지(F5 원칙) |
| G3 | P0 | ✅ **완료(2026-07-14)** — 4개 서비스 Deployment+Service(`k8s/base/`): probe 3종(F2), 리소스 산정표(F6), env 계약 CM/Secret 분리(F5), preStop 5s + 유예 40s 시간 사슬(F4), 웹 3종 maxUnavailable 0 롤링. Secret 실물 미커밋(생성 명령은 k8s/README.md, 관리 방식은 G8). 검증: kind에서 7파드 Ready, 시드 API·서비스간 재고 호출·Kafka consumer 조인·graceful 6.5s 실증. **발견**: shop-api Kafka consumer는 부팅 시 bootstrap DNS 해석 필수(브로커 다운은 재시도로 버티나 DNS 부재는 크래시) — Service `kafka` 존재가 기동 전제, G5에서 이름 유지 필수 | 검증용 임시 postgres/redis/kafka는 `k8s/dev/`(emptyDir) — G4/G5에서 대체 |
| G4 | P0 | ✅ **완료(2026-07-14)** — 로컬 Postgres/Redis를 **단순 StatefulSet+PVC로 확정(ADR-010)**, `k8s/overlays/local/` 소유(prod엔 미존재 — Supabase/Upstash). Bitnami 차트 배제(2025-08 Broadcom 무료 카탈로그 동결 + 로컬 단일 인스턴스엔 차트 가치 0). kind `standard`(local-path) PVC, PGDATA 하위 디렉토리, Redis AOF, postgres-init ConfigMap 계약 승계(G10 이관 전). 검증: 파드 삭제 후 데이터 생존(products·flyway·Redis 마커), 앱 커넥션 자동 복구·API 정상. `k8s/dev/`엔 kafka만 잔존 | 운영 상태ful 직접 운영 안 함 확정 |
| G5 | P1 | ✅ **완료(2026-07-14)** — **Strimzi Operator 1.1.0 확정(ADR-011)**: Helm으로 `strimzi` ns 설치(values는 `k8s/kafka/`), KafkaNodePool dual-role 1노드(KRaft, Kafka 4.3.0, PVC 2Gi). 운영(OKE)에서도 클러스터 내 직접 운영 대상이라 operator 경로의 가치 실재. bootstrap 계약은 env var라 ConfigMap만 `mini-commerce-kafka-bootstrap:9092`로 변경(G3 제약의 본질 = 부팅 시 DNS 해석 — Kafka CR을 앱보다 먼저 적용). 검증: consumer 조인·토픽 자동생성, 브로커 파드 삭제 후 토픽·메시지 생존·consumer 자동 복귀. `k8s/dev/` 소멸 | compose 단일 브로커의 승계 |
| G6 | P1 | ✅ **완료(2026-07-14)** — **ingress-nginx 4.15.1(ADR-012)**: Helm 설치(values는 `k8s/ingress-nginx/`, kind는 hostPort+control-plane 고정), 라우팅 규칙은 base 소유(`base/ingress.yaml`, 환경 공통 — host/TLS만 prod patch 예정). 최장 프리픽스 3규칙: `/api/admin/orders`→order-admin, `/api/orders`→order-api, `/api`→shop-api(`/api/admin/products`는 catalog admin이라 shop-api행), `/internal/**` 비노출(차단 강제는 G9). BFF는 env var 3개를 `http://localhost`로 주면 단일 진입 수렴(코드 무변경). CORS는 구도 동일로 무변경. 검증: nginx upstream 로그로 경로→포트 4종 확정, /internal 404 | 현재는 BFF가 포트별 직결 |
| G7 | P2 | HPA — shop-api/order-api 대상 CPU 기반부터, E6 부하 테스트로 임계값 산정. order-batch는 고정 replica | |
| G8 | P0 | ✅ **완료(2026-07-14)** — **.env 원천 스크립트 + SOPS/age 확정(ADR-013)**: `k8s/scripts/secrets.sh {apply\|seal\|apply-sealed}`, 암호화 커밋본 `k8s/secrets/app-secrets.enc.yaml`(값 필드만 ENC, 파생물 — 수동 편집 금지). **SealedSecrets 배제**(키가 클러스터에 살아 kind 재생성마다 봉인 무효 — 로컬 워크플로와 충돌). age 개인키 `~/.config/sops/age/keys.txt`(macOS는 sops 기본 경로가 달라 SOPS_AGE_KEY_FILE 명시), 리포 밖 백업 권장. 운영 비밀은 prod enc 분리, Argo CD ksops/ESO는 G11 재평가. 검증: seal 암호화·평문 잔존 grep·apply-sealed 왕복 바이트 일치 | B4 승계. 시크릿 평문 커밋 금지 |
| G9 | P1 | ✅ **완료(2026-07-14)** — **NetworkPolicy default-deny(Ingress)+명시 허용(ADR-014)**: `base/network-policy.yaml`(앱 4종 — 허용 규칙은 env var 계약과 1:1, ingress-nginx는 ns+pod 라벨로 식별) + `overlays/local/network-policy.yaml`(postgres/redis — 운영은 외부 SaaS라 local만 소유). Kafka는 Strimzi 자동 생성 정책 소관(추가 허용이라 default-deny와 공존). Egress는 의도적 미제한(위협 모델이 인바운드, DNS·외부 SaaS 재열거 비용 대비 이득 없음 — 운영 강화 때 재평가). kubelet probe·port-forward는 정책 미적용이라 생존. 검증: 전 파드 Ready 유지·/api 200·/internal 404, 긍정(shop-api→order-api/pg/redis/kafka, order-api→shop-api)·부정(무라벨→8080/8081/5432, shop-api→8082, default ns→8080 전부 차단) 통과. **주의: 미지원 CNI에선 조용히 무시 — OKE 전환 시 부정 테스트 재실행 필수** | B3의 인프라 레이어. H1 때 metrics 포트 허용 추가 필요 |
| G10 | P1 | ✅ **완료(2026-07-14)** — **orderdb 생성 Job 확정(ADR-015)**: `overlays/local/db-init-job.yaml`(멱등 SQL + pg_isready 대기 + ttl 600s 자동삭제 — apply마다 재실행 안전), G4 initdb ConfigMap(`postgres-init`) 제거. **initContainer 배제**(orderdb는 로컬 전용 관심사인데 order-api Deployment는 base 소유 — 소유권 위반 + 앱 파드에 CREATE DATABASE 자격증명 유입). Job 파드 라벨 `db-init`을 G9 allow-to-postgres에 추가. 검증: no-op 분기(기존 orderdb)·생성 분기(임시 DB로 동일 경로 실증)·ConfigMap 제거 후 롤링에도 PVC 데이터 생존·API 200. 전체 부트스트랩 리허설은 다음 kind 재생성 때 자연 검증. compose 쪽 `docker/postgres-init/`은 compose 폐기 시 함께 삭제. G11(Argo CD) 때 PreSync hook 재평가 | 스키마는 여전히 Flyway(order-api) 소유. 시드는 A6 Flyway db/seed로 종결 |
| G11 | P1 | 🔶 **1단계 완료(2026-07-14, 실전 검증 완료)** — **GitHub Actions kind 배포 검증(ADR-016)**: `.github/workflows/deploy-kind.yml` — kind(CI 단일노드 `cluster-ci.yaml`)+Strimzi+ingress-nginx 설치, compose 빌드→kind load, `overlays/ci`(local 상속+startupProbe 90회=180s+**cpu request 50m**, private 러너 2코어 보정) 배포, 롤아웃·orderdb-init 대기, 스모크(/api 200+시드, /internal 404, **G9 차단 회귀**). CI는 매번 빈 클러스터라 전체 부트스트랩(G10) 리허설을 매 실행 수행. Secret은 CI 더미(age 키 GH 미주입, ADR-013 정합). 트리거는 k8s/**·Dockerfile·compose 변경+수동(풀빌드 느려 매 푸시는 분수 낭비 — E5 후 재평가). **1차 실행에서 shop-api가 FailedScheduling(Insufficient cpu)으로 Pending 고착 발견** — 앱 4종+Kafka cpu request 합(950m)이 2코어(2000m) 러너 allocatable 초과. cpu limit이 원래 없는 설계(F6)라 request를 CI 전용으로 50m로 낮춰 해결(memory는 request=limit 유지 — JVM 힙 산정 기준이라 불변), Kafka는 kustomize 밖 리소스(`kubectl apply -f`)라 워크플로 내 `kubectl patch`로 별도 완화. **2차 실행 10분16초 전 스텝 성공**(kind 생성→Strimzi/ingress 설치→이미지 빌드/주입→배포→롤아웃→스모크→G9 회귀). **2단계(Argo CD)는 OKE 구축+E4 완료 후 착수** — ksops/ESO 결정 동반 | E4/E5가 빌드 스텝 개선의 선행. CI는 amd64 — arm64는 E4 소관. cpu request 50m는 CI 전용(overlays/ci) — local/prod는 원래 값(250m/100m) 유지 |

## H. 관측성 — k8s 통합 (5)

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| H1 | P1 | OTel Collector 도입 — 현재 앱→Tempo/Loki/Prometheus 직접 export를 Collector(Deployment/DaemonSet) 경유로 전환, 앱 설정은 endpoint 하나로 단순화 | k8s 리소스 메타데이터(pod/namespace) 라벨 자동 부착 |
| H2 | P1 | kube-prometheus-stack 도입 — 기존 Prometheus(OTLP push 전용) 통합, 노드/파드 메트릭 수집 | 현행 v2.55.1은 OTLP receiver가 experimental 플래그 — 이전 시 v3.x의 승격 여부 확인 필요 |
| H3 | P1 | Tempo/Loki Helm 배포 + PVC 스토리지/retention 설계 | compose 볼륨의 승계 |
| H4 | P2 | Grafana 대시보드 as code — 서비스별 RED 대시보드 + k8s 클러스터 대시보드 프로비저닝 | 현재 datasource만 프로비저닝됨 |
| H5 | P2 | Alertmanager 알림 규칙 — 에러율/컨슈머 랙/Pod 재시작 임계 알림 (Slack/Telegram) | |

## I. 운영 / 신뢰성 (4)

| # | P | 항목 | 근거/비고 |
|---|---|---|---|
| I1 | P0 | ~~운영 배포 구조와 MSA 분리의 괴리 해소~~ → **해소 경로 확정**: OCI k8s 이전(ADR-007)으로 G계열에 흡수. 이전 완료 전까지 운영 배포 동결, `render.yaml`은 이전 완료 시 제거 | ADR-007. 에픽 이슈로 추적 |
| I2 | P0 | ✅ **결정 완료 (2026-07-06, ADR-007)** — 운영을 OCI Always Free 기반 k8s로 이전. OKE Basic 우선, A1 용량 확보 실패 시 k3s 폴백. DB Supabase·Redis Upstash·프론트+BFF Vercel 유지 | k8s 전환 범위의 최상위 결정 — G계열 방향 확정 |
| I3 | P1 | Postgres 백업·복구 전략 — 로컬 k8s는 CronJob pg_dump, 운영은 Supabase 백업 정책 확인·복구 리허설 | |
| I4 | P2 | ✅ 프론트엔드 배치 — **Vercel 유지로 확정(ADR-007)**. BFF(Vercel)→백엔드(OCI) 리전 간 지연은 OCI 리전 선택 시 고려 | ADR-007 선결정 |

---

## 총 67개 — 착수 순서 제안

1. ~~**방향 결정 (P0 · 모든 트랙의 전제)**: I2(운영 환경 ADR)~~ ✅ **완료 (2026-07-06)** — OCI Always Free 기반 k8s 이전으로 확정, ADR-007 기록
2. **즉시 (P0 · 서비스 신뢰 기반)**: E1(CI) → D1/F3(Flyway) → B1/B2(RLS) → B3(internal 인증) → A6/F1(기동 결합 제거) → F2(probe)
3. **커머스 완성 (P0~P1)**: C1(PG 연동) → C2(환불, GH #4) → C3/C4(배송지·위시리스트 백엔드화)
4. **k8s 전환**: G1/G2(클러스터·매니페스트) → G3~G5(워크로드) → G8(Secret) → G6(Ingress) → G11(CD) → H계열
5. **학습 에픽 (P2)**: D3(inventory 분리 사가, GH #3)는 k8s 위에서 진행하면 배포·운영 학습 효과 극대화

> ⚠️ 순서의 핵심: **F계열(앱 k8s-ready)을 끝내기 전에 G계열(매니페스트)을 시작하지 말 것.**
> compose의 `depends_on` 봉합이 k8s에서 CrashLoopBackOff로 표면화되는 것이 전환 실패의 전형 패턴.
