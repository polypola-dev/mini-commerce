# ADR-006: 관측성 3대 축 완비 — Micrometer 브릿지 대신 OpenTelemetry 공식 스타터로 통일

- 상태: 승인 (2026-07-04)

## 컨텍스트

#7(Micrometer Tracing 구축)에서 트레이스 축만 우선 구현했으나, 애초 설계 의도는 Trace/Metric/Log
3축이었다. `micrometer-tracing-bridge-otel`로 order-api/order-admin/order-batch가 이미 트레이스를
Tempo로 보내고 있었지만, Micrometer Tracing은 트레이스 전용 추상화라 메트릭(Micrometer Meter
Registry)과 로그(Logback)는 각각 별도 통합이 필요해 3축을 한 벤더 스택으로 묶기 어려웠다.

shop-api에 OpenTelemetry 공식 스타터(`io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter`)를
스파이크로 먼저 검증한 결과(#7 후속):
- RestClient 자동계측이 기존 빈 주입 코드 그대로 호환됨.
- JDBC 쿼리 span이 추가 설정 없이 자동 캡처됨(Micrometer 브릿지 방식엔 없던 이득).
- shop-api(OTel 스타터) ↔ order-api(Micrometer 브릿지)가 W3C traceparent로 trace 연속성을
  유지 — 4개 모듈을 한 번에 안 바꾸고 순차 전환 가능함을 확인.

이 검증을 근거로, 나머지 3개 모듈까지 전환하고 Metric/Log 축을 추가할지, Micrometer를 유지한 채
메트릭/로그만 별도로 붙일지 결정이 필요했다.

## 결정

1. **4개 백엔드 모듈(shop-api/order-api/order-admin/order-batch) 전부를 Micrometer Tracing
   Bridge에서 OpenTelemetry 공식 스타터로 전환**한다. Micrometer 브릿지와 OTel 스타터를 한
   프로세스에 병행하면 OTel SDK 오토컨피그가 충돌하므로 공존 금지, 항상 스타터 단독 사용한다.
2. **Trace/Metric/Log 각 축을 서로 다른 백엔드로 개별 export**한다. 공용 endpoint
   (`otel.exporter.otlp.endpoint`)는 시그널별 well-known path(`/v1/traces` 등)를 자동으로 붙여주는
   편의 기능이 있지만, 이는 "하나의 수신처"를 전제로 한 것이라 백엔드가 갈리는 우리 구성엔 안 맞는다.
   대신 `otel.exporter.otlp.{traces,metrics,logs}.endpoint`로 시그널별 endpoint를 명시하며, 이
   경우엔 자동 경로 부착이 없어 전체 경로(`/v1/traces`, `/otlp/v1/logs`, `/api/v1/otlp/v1/metrics`)를
   직접 명시해야 한다.
3. **Collector 없이 앱이 각 백엔드(Tempo/Loki/Prometheus)에 직접 push**한다. 로컬 개발 단일
   인스턴스에는 Collector가 관리 포인트만 늘리는 오버엔지니어링 — SaaS export가 필요해지면 그때
   재검토한다(#7의 F1 참고).
4. **로그 축**: `opentelemetry-logback-appender-1.0`을 콘솔 appender와 병행 등록한다
   (`logback-spring.xml`에 Spring Boot 기본 `defaults.xml`/`console-appender.xml`을 include하고
   `OpenTelemetryAppender`를 추가). 이 아티팩트는 **alpha 계열로만 배포**되어 안정 BOM
   (`opentelemetry-instrumentation-bom`)에는 버전이 없고, `opentelemetry-instrumentation-bom-alpha`를
   함께 import해야 해결됨을 실빌드 중 발견했다.
5. **메트릭 축**: Prometheus를 OTLP push 수신 전용으로 구성한다(scrape_configs 없음). Prometheus
   공식 문서는 `--web.enable-otlp-receiver`를 stable 플래그로 안내하지만, 우리가 고정한 `v2.55.1`
   에는 아직 없고 experimental feature flag(`--enable-feature=otlp-write-receiver`)로만 존재함을
   실기동 중 확인했다 — 향후 Prometheus를 stable-flag 지원 버전으로 올리면 플래그명을 이관해야 한다.
6. **Grafana trace-to-logs 연동은 로그 본문 정규식이 아니라 구조화 메타데이터(label) 기반**으로
   구성한다. 당초 로그 콘솔 패턴(`[app,traceId,spanId]`)에서 정규식으로 traceId를 추출하려 했으나,
   OTel logback appender가 Loki로 보내는 로그는 **메시지 본문만** 담고 trace_id/span_id는 OTLP
   LogRecord의 별도 필드로 실려 Loki에 **구조화 메타데이터**로 저장된다는 걸 실측으로 확인했다
   (본문 정규식 방식은 매칭 자체가 안 됨). Grafana Loki datasource의 `derivedFields`를
   `matcherType: label, matcherRegex: trace_id`로 구성해 이 구조화 메타데이터를 직접 참조한다.

## 대안

- **Micrometer 유지 + 메트릭/로그 별도 통합**(Micrometer Meter Registry → Prometheus, Logback
  별도 appender): 이미 검증된 트레이스 경로를 안 건드려도 됨. 그러나 3개 벤더 조합(Micrometer
  Tracing / Micrometer Metrics / 별도 로그 라이브러리)을 개별 관리해야 하고, JDBC span 자동
  계측 등 OTel 스타터의 부가 이득을 놓침 → 기각(스파이크에서 이미 OTel 스타터 우위 확인).
- **OTel Collector 경유**: 앱은 Collector 하나에만 export하고 Collector가 Tempo/Loki/Prometheus로
  라우팅. 신호 조작(필터/샘플링/변환)이 필요해지면 유리하나, 로컬 1인 개발에 관리 포인트
  (설정/배포/장애점)만 늘림 → 기각(향후 SaaS export 시 재검토, #7 F1과 연계).
- **Prometheus remote_write / Micrometer registry 유지**: pull 기반 scrape가 표준적이고 OTLP
  push는 공식 문서상 "저볼륨 전용" 권고사항이 있다 — 그러나 로컬 개발 규모에선 문제되지 않고,
  3축을 동일 export 메커니즘(OTLP)으로 통일하는 운영 단순성 이득이 더 커 채택.

## 결과

- 4개 백엔드 모듈이 전부 동일한 관측성 스택(OTel 공식 스타터)을 쓰게 되어, 향후 신규 서비스 추가
  시 관측성 설정을 템플릿화할 수 있다.
- Trace/Metric/Log 3축이 각각 Tempo/Prometheus/Loki에 실제 도착함을 컨테이너 기동 검증으로
  확인했다(트레이스: order-api 트레이스 조회 성공, 로그: 4개 서비스 `service_name` 라벨로 조회
  성공, 메트릭: 4개 서비스의 JVM/HTTP 메트릭 조회 성공).
- 트레이드오프: 로그 appender가 alpha BOM에 의존, Prometheus의 OTLP push 수신이 experimental
  기능(향후 stable 승격 시 플래그명 이관 필요), 로그 축의 trace 연계가 "구조화 메타데이터
  라벨"이라는 상대적으로 덜 직관적인 방식에 의존(다만 Grafana/OTel 공식 권장 패턴).
- 진행 체크리스트 = **GitHub Issue #8**(완료). 선행 스파이크 = **#7**.

## 관련 이슈
- GitHub Issue #7 (Trace 축, 완료) — 선행 스파이크
- GitHub Issue #8 (Metric/Log 축 + 4개 모듈 전체 통합, 완료)
