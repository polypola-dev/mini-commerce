# 로컬 관측성 스택 (Tempo + Loki + Prometheus + Grafana)

GH #7/#8에서 구축한 로컬 전용 관측성 스택 사용법. Trace/Metric/Log 3축을 각 앱이
Collector 없이 직접 OTLP로 export하고, Grafana에서 한 화면에 조회한다.

## 기동

```bash
docker compose up -d
```

브라우저에서 Grafana 접속: http://localhost:3001 (익명 Admin 접근, 로그인 불필요)

| 서비스 | 컨테이너 내부 | 호스트 포트 | 용도 |
|---|---|---|---|
| Grafana | grafana:3000 | 3001 | 조회 UI |
| Tempo | tempo:4318(OTLP)/3200(조회 API) | 4317/4318/3200 | 트레이스 저장소 |
| Loki | loki:3100 | 3100 | 로그 저장소(OTLP 네이티브 수신) |
| Prometheus | prometheus:9090 | 9090 | 메트릭 저장소(OTLP push 수신) |

4개 백엔드 모듈(shop-api/order-api/order-admin/order-batch) 모두
`opentelemetry-spring-boot-starter`가 앱 기동과 동시에 3축을 자동으로 export한다.
별도 코드 계측 없이 HTTP 요청/JDBC 쿼리/JVM 상태가 자동으로 잡힌다.

## Grafana에서 조회하기

좌측 메뉴 **Explore**에서 상단 datasource 드롭다운으로 Tempo/Loki/Prometheus를 전환한다.

### Tempo (트레이스)

- Search 탭에서 `Service Name`을 `mini-commerce-backend`(shop-api) 또는
  `mini-commerce-order-api` / `mini-commerce-order-admin` / `mini-commerce-order-batch`로 선택 후 검색.
  (서비스명은 각 모듈 `application.yml`의 `otel.service.name` 기본값.)
- 트레이스를 클릭하면 서비스 간 span(예: shop-api → order-api RestClient 호출)이 하나의
  타임라인에 이어져 보인다 — W3C traceparent 전파 결과.

### Loki (로그)

LogQL 예시:

```logql
{service_name="mini-commerce-order-api"}
```

- `service_name` 라벨로 서비스별 로그를 구분한다.
- 로그 목록에서 개별 항목을 펼치면 **Fields** 영역에 구조화 메타데이터
  (`trace_id`, `span_id`, `severity_text` 등)가 보인다. `trace_id`가 있는 로그 줄에는
  **"Tempo에서 트레이스 보기"** 링크가 자동으로 붙는다 — 클릭하면 바로 해당 트레이스로 이동한다
  (trace-to-logs 연동).
  - 부팅 로그처럼 활성 span 없이 찍힌 줄에는 `trace_id`가 없어 링크가 안 붙는 게 정상이다.
    실제 HTTP 요청을 처리하는 동안(활성 span 내부) 찍힌 로그에만 trace_id가 실린다.

### Prometheus (메트릭)

PromQL 예시:

```promql
jvm_memory_used_bytes{job="mini-commerce-order-api"}
http_server_request_duration_seconds_count{job="mini-commerce-order-api"}
```

- `job` 라벨이 서비스 구분자다(OTel의 `service.name` 리소스 속성이 매핑됨. Loki의
  `service_name`과 이름이 다르니 헷갈리지 않도록 주의).
- scrape가 아니라 앱이 주기적으로(기본 60초) push하는 방식이라, 앱 기동 직후 1분 정도는
  데이터가 비어있을 수 있다.

## 트러블슈팅

- **컨테이너 기동 순서 때문에 초반 export 에러 로그(Failed to export ...)가 보일 수 있다.**
  Tempo/Loki/Prometheus보다 백엔드 앱이 먼저 뜨면 DNS 조회가 한 번 실패하고, OTel SDK가
  자동 재시도하므로 몇 초 후 정상화된다. 계속 반복되면 `docker compose restart <서비스명>`으로
  해당 백엔드 앱을 재기동한다.
- **Prometheus가 기동 실패하며 `unknown long flag '--web.enable-otlp-receiver'`를 낸다면**:
  이미지 버전을 확인한다. 우리가 고정한 `prom/prometheus:v2.55.1`은 아직 이 플래그가 stable로
  승격되지 않아 `--enable-feature=otlp-write-receiver`(experimental)로 활성화한다
  (`docker-compose.yml` 참고). Prometheus를 더 최신 버전으로 올릴 경우 플래그명을 확인하고 이관한다.
- **Grafana에서 datasource가 "data source not found" 에러로 기동 실패하면**: 로컬에만 존재하는
  Grafana 내부 상태(데이터소스 uid 등)가 `observability/grafana/provisioning/datasources/*.yaml`과
  어긋난 것이다. Grafana는 대시보드/데이터를 영속하지 않는 순수 조회 도구이므로
  `docker compose rm -sf grafana && docker compose up -d grafana`로 안전하게 초기화할 수 있다.

## 로컬 전용, 프로덕션 무관

이 스택은 로컬 개발 전용이며 Render 등 실제 배포 환경에는 배포되지 않는다($0 비용 유지).
프로덕션에서 SaaS 관측성 백엔드로 export가 필요해지면 그때 OTel Collector 도입을 재검토한다.
