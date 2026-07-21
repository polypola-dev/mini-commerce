# 설정 외부화 계약 (F5) — 환경변수 · 프로파일 · 리소스 산정

> k8s 전환(G계열)에서 ConfigMap/Secret/매니페스트를 작성할 때 이 문서가 입력값이다.
> **docker-compose.yml의 `environment` 블록과 이 문서가 어긋나면 이 문서를 갱신한다** —
> compose가 사실상의 계약 원본이고, 이 문서는 그 계약의 k8s 매핑을 정의한다.

## 원칙

- **환경차는 전부 환경변수로 흡수한다** (12-factor). 같은 이미지가 local compose / k8s /
  운영에서 env만 바꿔 그대로 돈다. 코드·이미지에 환경 지식(호스트명, 키)을 심지 않는다.
- application.yml의 모든 외부값은 `${ENV_VAR:default}` 형태 — default는 **로컬 개발
  (호스트에서 직접 `bootRun`) 기준**이고, 컨테이너 환경은 env로 전부 덮어쓴다.
- Spring 프로파일은 환경 분기 수단이 아니다(아래 "프로파일 전략").

## 프로파일 전략

| 프로파일 | 용도 | 활성화 위치 |
|---|---|---|
| (기본, 무프로파일) | 모든 환경의 기본 동작. 환경차는 env var로만 | k8s/운영 포함 전부 |
| `local` | **shop-api 전용** — Flyway `db/seed`(더미 상품) 추가 로드 (GH #14) | docker-compose의 shop-api만 |

**`k8s`/`prod` 프로파일은 만들지 않는다 (F5 결정).** 프로파일 분기는 "이 코드는 어느 환경에서
도는가"라는 지식을 아티팩트에 심는 것이라 env var 계약과 중복되고, 프로파일 조합 누락이
런타임에야 터진다. 유일한 예외인 `local`은 환경 분기가 아니라 **시드 데이터 on/off 스위치**다.
k8s에서 시드가 필요한 로컬 클러스터(kind)라면 `SPRING_PROFILES_ACTIVE=local`을 env로 주면 된다.

## 환경변수 계약 (서비스별)

k8s 매핑: **CM** = ConfigMap, **SEC** = Secret, **이미지** = Dockerfile ENV 기본값 사용(필요 시만 덮어씀).

### 공통 (5개 서비스 전부)

| 변수 | 용도 | 기본값(yml) | k8s |
|---|---|---|---|
| `DATABASE_URL` | JDBC URL (shop-api→minicommerce, order-*→orderdb, inventory-api→inventorydb) | localhost | CM |
| `DATABASE_USERNAME` | DB 계정 | minicommerce | SEC |
| `DATABASE_PASSWORD` | DB 비밀번호 | minicommerce | SEC |
| `REDIS_HOST` / `REDIS_PORT` | Redis 접속 | localhost:6379 | CM |
| `REDIS_PASSWORD` | Redis 비밀번호 (Upstash 대비) | (빈값) | SEC |
| `REDIS_SSL` | Redis TLS (Upstash 대비) | false | CM |
| `SERVER_PORT` | 서비스 포트 | 8080~8084 | CM |
| `OTEL_SERVICE_NAME` | 텔레메트리 서비스명 | 모듈별 기본값 | CM |
| `OTLP_TRACES_ENDPOINT` / `OTLP_METRICS_ENDPOINT` / `OTLP_LOGS_ENDPOINT` | 시그널별 OTLP 수신처 (H1에서 Collector 단일 endpoint로 단순화 예정) | tempo/prometheus/loki | CM |
| `JAVA_OPTS` | JVM 플래그 통째 교체 (F6) | 이미지: `-XX:MaxRAMPercentage=75.0` | 이미지 |

### shop-api

| 변수 | 용도 | k8s |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local`일 때만 시드 로드 (위 프로파일 전략) | CM (로컬 클러스터만) |
| `KAFKA_BOOTSTRAP_SERVERS` | notification 컨슈머 | CM |
| `INVENTORY_BASE_URL` | catalog→inventory-api 내부 재고 API (GH #3, ADR-019) | CM |
| `CORS_ALLOWED_ORIGINS` | 브라우저 직접 호출 허용 오리진 (B8에서 축소 검토) | CM |
| `SUPABASE_JWKS_URL` | JWT 서명 검증 | CM |
| `BFF_SECRET_KEY` | BFF 게이트웨이 검증 키 | SEC |
| `SUPABASE_URL` | Supabase 프로젝트 URL | CM |
| `SUPABASE_SERVICE_ROLE_KEY` | Supabase admin API 키 | SEC |
| `INTERNAL_API_KEY` | 서비스간 `/internal` 인증 (B3, ADR-020) — 수신(catalog)·발신(→inventory-api) 겸용 | SEC |

### order-api

| 변수 | 용도 | k8s |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | order 이벤트 발행 | CM |
| `CATALOG_BASE_URL` | order→shop-api 상품 조회 | CM |
| `CORS_ALLOWED_ORIGINS` | " | CM |
| `SUPABASE_JWKS_URL` | " | CM |
| `BFF_SECRET_KEY` | " | SEC |
| `TOSS_SECRET_KEY` | 토스페이먼츠 결제 승인 API Basic 인증 시크릿 키 (C1) | SEC |
| `INVENTORY_BASE_URL` | order→inventory-api 예약 사가 REST (GH #3) | CM |
| `INTERNAL_API_KEY` | 서비스간 `/internal` 인증 (B3, ADR-020) — **발신 전용**(order-api는 `/internal` 미노출) | SEC |

### order-admin

order-api와 동일하되 **Kafka 없음** (`KAFKA_BOOTSTRAP_SERVERS` 불필요 — 이벤트 발행/소비 안 함).
`CATALOG_BASE_URL`/`INVENTORY_BASE_URL`은 현재 미호출이지만 order-infra 전이의존으로 조립되므로
명시 유지(compose 주석 참고). `TOSS_SECRET_KEY`(SEC)는 관리자 주문취소가 Toss 환불을 실행하므로
필요(GH #4) — order-api와 동일 값. 재입고는 order.canceled 발행으로 위임(S4 코레오그래피)이라
inventory-api를 동기 호출하지 않는다. `INTERNAL_API_KEY`(SEC)는 order-api와 동일하게 발신용으로
필요하다(B3, ADR-020).

### order-batch

| 변수 | 용도 | k8s |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | 미발행 이벤트 스윕 재발행 + `inventory.reservation.expired` 구독 | CM |
| `CATALOG_BASE_URL` | order-infra 전이의존 조립용 | CM |
| `INVENTORY_BASE_URL` | order-infra 전이의존 조립용(미호출) | CM |
| `INTERNAL_API_KEY` | order-infra 전이의존 조립용(미호출) — RestClient 빈이 요구 (B3, ADR-020) | SEC |

웹 API가 없어 `CORS_ALLOWED_ORIGINS`/`SUPABASE_JWKS_URL`/`BFF_SECRET_KEY` 불필요.

### inventory-api

| 변수 | 용도 | k8s |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `order.paid`/`order.canceled` 구독 + `inventory.reservation.expired` 발행 | CM |
| `INTERNAL_API_KEY` | 서비스간 `/internal` 인증 (B3, ADR-020) — 이 서비스의 **유일한** 앱 레이어 방어 | SEC |

전용 `inventorydb`(Flyway 단독 소유). 외부(ingress) 미노출 — 전부 `/internal`이라
`CORS_ALLOWED_ORIGINS`/`SUPABASE_JWKS_URL`/`BFF_SECRET_KEY`/`TOSS_SECRET_KEY` 불필요(shared-web 미의존).

## Graceful shutdown 계약 (F4)

종료 신호가 앱까지 전달되고, 앱이 정리를 마칠 때까지 죽이지 않는 시간 사슬:

```
SIGTERM → java(PID 1, Dockerfile exec) → Spring graceful 단계(≤30s) → 정상 종료
                                          └ 초과 시에만 컨테이너 유예(35s) 후 SIGKILL
```

| 레이어 | 설정 | 값 |
|---|---|---|
| 앱 | `spring.lifecycle.timeout-per-shutdown-phase` | **30s** |
| 앱 | `server.shutdown: graceful` (Boot 3.4+ 기본이지만 명시) | — |
| order-batch, inventory-api | `spring.task.scheduling.shutdown.await-termination(-period)` | true / 25s (앱 유예보다 짧게 — 리퍼/스케줄 작업 정상 완료) |
| compose | `stop_grace_period` | **35s** (> 앱 유예) |
| k8s (G3에서 적용) | `terminationGracePeriodSeconds` | **40s** = preStop(5s) + 앱 유예(30s) + 여유 |
| k8s (G3에서 적용) | `preStop: sleep 5` | Endpoint 제거 전파 지연 동안 신규 트래픽 수신 커버 |

값을 바꿀 때는 **앱 유예 < 컨테이너 유예** 부등식을 유지한다. 반대가 되면 graceful이
SIGKILL로 무효화되고 롤링 업데이트마다 처리 중 요청이 유실된다.

## 리소스 산정 (F6) — G3 Deployment 입력값

전제: 이미지 공통 `-XX:MaxRAMPercentage=75.0` → 힙 최대 = memory limit × 0.75.
compose `mem_limit: 1g`로 로컬에서 동일 조건 상시 검증 중.
실측(2026-07-13, 무제한 상태 RSS): shop-api 983Mi, order-api 834Mi, order-admin 833Mi, order-batch 777Mi
— 기본 힙 정책(RAM 25%≈2GiB)에서 GC 압력 없이 부풀려진 값으로, limit 하에서는 GC가 회수한다.

| 서비스 | memory request=limit | CPU request | CPU limit | 비고 |
|---|---|---|---|---|
| shop-api | 1Gi | 250m | **없음** | BFF 트래픽 최다 + notification 컨슈머 |
| order-api | 1Gi | 250m | 없음 | 재고 Lua/이벤트 발행 |
| order-admin | 1Gi | 100m | 없음 | 관리 트래픽 소량 |
| order-batch | 1Gi | 100m | 없음 | 스케줄 작업만, HPA 제외 대상(고정 replica) |
| inventory-api | 1Gi | 250m | 없음 | 재고 Lua/예약 사가 REST + order 이벤트 컨슈머 + 만료 리퍼 |

- **memory는 request=limit** (Guaranteed에 준하게) — JVM은 limit 기준으로 힙을 잡으므로
  request<limit 오버커밋은 노드 압박 시 OOMKill 우선순위만 높인다.
- **CPU limit은 두지 않는다** — JIT가 몰리는 부팅 구간 burst를 스로틀하면 기동·readiness만
  느려진다(ROADMAP F6 "부팅 CPU burst 고려"). CPU request는 스케줄링 힌트로만.
- OCI Always Free(A1 24GiB) 기준 4×1Gi + 인프라로 여유 충분. 조정은 H2(kube-prometheus-stack)
  도입 후 실측 기반으로 — 먼저 낮추지 말 것(OOMKill 단골 경로).
