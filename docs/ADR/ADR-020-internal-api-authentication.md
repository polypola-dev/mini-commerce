# ADR-020: 서비스간 `/internal` 인증 — 공유 시크릿 헤더

- 상태: 승인 (2026-07-21)
- 관련: ADR-014(NetworkPolicy — 본 ADR의 네트워크 레이어 짝), ADR-012(ingress-nginx —
  `/internal` 비노출), ADR-011(Kafka listener 무인증 plaintext — 아래 "남는 부채"),
  ADR-013(SOPS/age 시크릿), ADR-019(inventory 완전분리로 `/internal` 표면 확대),
  ROADMAP B3, GH #13, backend/doc/CONFIGURATION.md(env var 계약)

## 컨텍스트

`/internal/**`은 서비스간 전용 API인데 **앱 레이어 신원 검증이 전혀 없었다.** 인증 필터가
URL 패턴 화이트리스트 방식(`addUrlPatterns("/api/...")`)이라 `/internal`은 애초에 필터 체인에
걸리지 않았고, inventory-api는 `shared-web`을 의존하지 않아 필터 자체가 없었다.

현재 노출 표면(REST 3간선 / 오퍼레이션 7개):

| 수신 | 엔드포인트 | 호출자 |
|---|---|---|
| shop-api(catalog) | `GET /internal/products/{id}`, `/options/{id}` | order-api |
| inventory-api | `GET /internal/inventory/stocks`, `PUT /internal/inventory/stock/{id}` | shop-api |
| inventory-api | `POST`·`DELETE /{orderId}`·`GET /{orderId}` `/internal/inventory/reservations` | order-api |

방어는 네트워크 레이어(ADR-014 default-deny + ADR-012 Ingress 미라우팅)뿐이었다. 이는
**단일 방어선**이고, ADR-014 자신이 "미지원 CNI에선 조용히 무시된다"를 경고하고 있다. 즉
정책이 무력화되면 같은 네임스페이스의 임의 파드가 재고를 임의 조작하거나 예약 사가를 조작할
수 있다. ADR-019로 inventory가 별도 서비스가 되면서 이 표면이 실제로 커졌다.

## 결정

**`X-Internal-Key` 공유 시크릿 헤더 + `InternalAuthFilter`(shared-web)를 도입한다.**
불일치 시 **404**로 응답하고, 허용 키는 **콤마 구분 목록**으로 받아 무중단 로테이션을 지원한다.

### 왜 공유 시크릿인가 (mTLS/서비스 JWT 대신)

| 안 | 기각 사유 |
|---|---|
| 서비스 JWT | 호출자가 3개뿐인 신뢰 도메인이라 호출자별 신원의 실익이 작다. 발급자·키 배포 문제가 재발해 순비용이 크다. |
| mTLS / 서비스 메시 | OCI free tier(Ampere A1) 리소스에 istiod+사이드카는 현실성이 낮다. 이미 kube-prometheus-stack·Tempo·Loki·Strimzi가 올라가 있다. |

핵심 판단은 **위협 모델**이다. 막으려는 것은 "네임스페이스 안에 침입한 임의 파드"이지
"신뢰 서비스 중 하나의 배신"이 아니다. 전자에는 공유 시크릿으로 충분하고, 후자를 상정하면
mTLS로도 부족하다(인증서를 가진 서비스가 배신하는 경우).

### 설계 세부와 근거

- **404 (401/403 아님)** — 엔드포인트 존재 자체를 드러내지 않는다. 외부에서 본 `/internal`은
  "라우팅 없음"이어야 하고(ADR-012), ADR-014의 부정 테스트도 이미 404를 차단 판정 기준으로
  쓰고 있어 관측 계약이 일관된다.
- **SHA-256 다이제스트 비교** — 원문 대신 고정 길이 해시를 `MessageDigest.isEqual`로 비교한다.
  `isEqual`은 길이가 다르면 조기 반환하므로 원문 비교는 키 길이를 누출할 여지가 있다. 허용
  키 목록도 첫 일치에서 빠져나가지 않는다(목록 내 위치 누출 방지).
- **복수 키 허용** — 신키 추가 배포 → 발신측 전환 → 구키 제거의 3단 배포로 어느 시점에도
  거부되는 조합이 없다.
- **기동 시 fail-fast** — 허용 키가 비면 전 호출이 조용히 404가 되어 원인 추적이 어렵다.
  설정 누락은 부팅에서 실패시킨다.
- **`/actuator/**`는 대상 밖** — F2의 liveness/readiness probe는 kubelet이 무인증으로
  접근해야 한다. 필터는 `/internal/*`에만 등록한다.

### 배치

- 수신 필터: shop-api, inventory-api **둘뿐**. order-api는 `/internal`을 노출하지 않는
  **발신 전용**이다.
- 헤더 이름 상수는 `shared-core`의 `InternalApiContract` — 발신측(catalog, order-infra)과
  수신측(shared-web)이 공통으로 의존하는 유일한 지점이고, 순수 상수라 "spring 의존 금지"
  원칙과 충돌하지 않는다.
- inventory-api는 필터 재사용을 위해 `shared-web`을 의존한다. 설정 클래스는
  `com.minicommerce.global`에 둔다 — 부팅 베이스가 `com.minicommerce`라 Modulith가 `global`을
  모듈로 보고 `global.security`는 비노출 하위 패키지이기 때문이다(다른 패키지에서 참조하면
  `ModularityVerificationTest`가 깨진다. shop-api/order-api의 `WebConfig`가 같은 이유로
  이 패키지에 있다).
- `INTERNAL_API_KEY`는 SEC 등급 — `.env` 원천 + SOPS 봉인(ADR-013). order-batch도 값이
  필요한데, 호출하지 않지만 order-infra 전이의존으로 RestClient 빈이 조립되기 때문이다
  (`CATALOG_BASE_URL`과 같은 사유).

## 결과

- 네트워크(ADR-014) + 앱(본 ADR) 2중 방어가 완성됐다. 어느 한쪽이 무력화돼도 즉시
  털리지 않는다.
- 롤아웃은 **2단 분리**가 필요하다. 발신(헤더 전송)과 수신(검증 강제)을 같은 배포에 넣으면
  롤링 업데이트 중 "신규 수신 파드 + 구버전 발신 파드" 조합에서 404가 난다.
- 발신측 헤더 부착은 `@Configuration`에 있어 기존 어댑터 테스트(자체 Builder 사용)가
  지나가지 않는다. 설정 클래스를 직접 호출하는 테스트를 3간선 각각에 추가했다 — 누락 시
  런타임에만 드러나는 종류의 결함이다.

## 한계 (의식적으로 수용)

공유 시크릿은 **베어러 토큰**이다. 다음을 제공하지 못한다:

- **호출자별 신원** — 어느 서비스가 호출했는지 구분되지 않는다. 감사 요구가 생기면 서비스
  JWT를 이 위에 증분으로 얹어야 한다.
- **만료** — 키는 명시적으로 로테이션하기 전까지 유효하다.
- **유출 시 즉시성** — 키가 새면 로테이션 완료 전까지 무방비다. 로그·트레이스에 헤더가
  실리지 않는지가 실질적 방어선이 된다.

## 남는 부채: Kafka 축

서비스간 신뢰 표면은 REST 3간선 + **Kafka 3간선**(order→shop-api notification,
order→inventory-api, inventory→order-infra)이고, **본 ADR은 앞쪽 절반만 덮는다.**
Kafka listener는 여전히 무인증 plaintext(ADR-011)이며 그 전제도 NetworkPolicy 하나다 —
`/internal`이 갖고 있던 것과 **구조적으로 동일한 부채**가 그대로 남는다.

Strimzi SASL/mTLS 리스너 + 앱 5종 클라이언트 설정이라 작업 성격과 규모가 달라 분리한다.
서비스 메시 도입 여부와도 얽히므로 **구현 여부는 미확정**으로 ROADMAP에 남긴다.
