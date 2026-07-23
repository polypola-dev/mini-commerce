# E6 — 재고 예약(reserve) 동시성 부하 테스트

ROADMAP.md E6: "k6 부하 테스트 — 재고 동시성(reserve/confirm/release) 시나리오, HPA 튜닝(G7)
근거 데이터, MSA 분리 검증 겸용".

## 스코프 (왜 confirm/release는 이 스크립트에 없는가)

- **reserve**: `POST /api/orders` (order-api → inventory-api 동기 호출) — 오버셀 방지가
  Redis Lua 스크립트 원자성에 달려있는 실제 경합 지점. 이 스크립트의 핵심 대상.
- **confirm**: REST 엔드포인트가 없다. 실제 Toss 결제 확인(`/api/orders/{id}/confirm-payment`)
  후 `order.paid` Kafka 이벤트로만 트리거된다. 부하 규모로 반복 호출하면 Toss 테스트
  샌드박스를 대량으로 때리게 되어 이 스크립트에서 의도적으로 제외했다.
- **release**: `/cancel`은 PAID 주문에만 허용된다(`OrderService.doCancel`, 결제 전 취소 API
  자체가 없음). 결제 전 예약은 10분 TTL이 지나야 `ExpiredReservationReleaser`(60초 주기)가
  회수한다 — 아래 "release 확인" 절차 참고.

## 사전 준비

```bash
brew install k6
docker compose up -d   # shop-api(18080)/order-api(18081)/inventory-api(18084) 등
```

루트 `.env`에 다음 값이 있어야 한다: `SUPABASE_SERVICE_ROLE_KEY`, `BFF_SECRET_KEY`,
`INTERNAL_API_KEY`. 스크립트가 알아서:
1. `SUPABASE_SERVICE_ROLE_KEY`로 전용 테스트 계정(`k6-loadtest@mini-commerce.internal`)을
   생성(이미 있으면 재사용)하고 로그인해 JWT를 발급받는다.
2. 대상 상품(기본: 시드 데이터의 Desk Lamp, `00000000-0000-7000-8000-000000000003`)의
   재고를 inventory-api Redis에 `PUT /internal/inventory/stock/{id}`로 매 실행마다 결정적인
   값(기본 30)으로 초기화한다 — 이 시드 상품은 catalog 경유 생성이 아니라 SQL로 바로
   꽂힌 데이터라 inventory-api 쪽엔 원래 재고 레코드가 없다.

## 실행

```bash
./loadtest/run.sh
# VU/반복 수 조정
./loadtest/run.sh -e VUS=100 -e ITERATIONS=200 -e INITIAL_STOCK=50
```

기본값: VUS=30, ITERATIONS=60(재고의 2배 — 절반은 성공, 절반은 품절을 의도적으로 유도해
양쪽 경로 모두 부하 조건에서 검증). `order_out_of_stock` 카운터가 올라가는 건 정상이고,
`order_unexpected_error`(4xx/2xx 외 응답)가 0이어야 통과다.

teardown 단계에서 inventory-api 남은 재고를 조회해 로그로 남긴다.
`remainingStock`이 음수면 오버셀 — 절대 발생하면 안 되는 상태다.

## 검증 포인트 (실행 후 직접 확인)

- **오버셀 0건**: teardown 로그의 `remainingStock >= 0` 확인. 정확한 값은
  `max(0, INITIAL_STOCK - order_success 카운트)`와 같아야 한다.
- **서킷브레이커 오탐 없음** (D6/ADR-021 미검증 이월 항목과 연결): 품절 4xx가 대량으로
  발생해도 order→inventory 서킷은 CLOSED로 남아야 한다.
  ```bash
  curl -s localhost:18081/actuator/circuitbreakers | jq
  ```
- **release 확인** (11분 이상 대기 필요 — TTL 10분 + 리퍼 주기 최대 60초):
  ```bash
  curl -s -H "X-Internal-Key: $INTERNAL_API_KEY" \
    "localhost:18084/internal/inventory/stocks?ids=00000000-0000-7000-8000-000000000003"
  ```
  burst 직후엔 `min(INITIAL_STOCK, order_success)`만큼 소진된 값이 보이고, 결제로 이어지지
  않은 예약은 11분 뒤 이 값이 원래 재고로 복원돼야 한다(단, 이번 시나리오는 주문을
  confirm하지 않으므로 전부 결제 전 상태 — 사실상 전량이 TTL 만료 대상).

## kind/OKE 대상으로 실행하려면 (G7 HPA 근거 데이터)

로컬 docker-compose는 리소스 제약이 kind/OKE와 다르므로, G7 임계값 산정은 실제로는
kind나 OKE 클러스터에 대해 재실행해야 의미가 있다. 실측 확인된 절차(2026-07-23):

```bash
# /internal/**은 ingress에 비노출(ADR-012/014)이라 setup()의 재고 초기화만 별도 경로 필요
kubectl port-forward svc/inventory-api 18084:8084 -n mini-commerce &

./loadtest/run.sh \
  -e ORDER_API_URL=http://localhost \
  -e INVENTORY_API_URL=http://localhost:18084
```

**주의**: `ORDER_API_URL`엔 `/api`를 붙이지 않는다 — 스크립트가 `${ORDER_API_URL}/api/orders`로
자체 조립하므로, ingress 주소가 이미 `/api`를 포함한 형태(`http://<host>/api`)로 주면
`/api/api/orders`가 되어 404가 난다(docker-compose 기본값도 같은 이유로 포트만 지정).

그때 CPU/메모리 사용량은 Grafana RED 대시보드(H4)에서 같은 시간대를 확인한다.
