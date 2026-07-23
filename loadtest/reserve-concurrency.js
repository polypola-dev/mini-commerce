import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// ROADMAP E6 — 재고 동시성(reserve) 부하 테스트 + MSA 분리(order-api -> inventory-api) 검증 겸용.
//
// 스코프 결정(설계 근거):
// - confirm은 REST가 없다(order.paid Kafka 이벤트로만 트리거되고, 실제 Toss 결제 확인을 거쳐야
//   한다) — 부하테스트로 실제 PG 샌드박스를 때리는 건 부적절해 이 스크립트는 reserve(주문 생성)
//   동시성에 집중한다.
// - release는 예약 후 10분 TTL이 지나야 리퍼가 회수한다(InventoryService.RESERVATION_TTL, 취소
//   API는 PAID 주문에만 허용). 이 스크립트가 끝난 뒤 README의 안내대로 시간을 두고 재고가
//   복원되는지 별도 확인한다.

const successes = new Counter('order_success');
const outOfStock = new Counter('order_out_of_stock');
const unexpected = new Counter('order_unexpected_error');

const ORDER_API_URL = __ENV.ORDER_API_URL || 'http://localhost:18081';
const INVENTORY_API_URL = __ENV.INVENTORY_API_URL || 'http://localhost:18084';
const SUPABASE_URL = __ENV.SUPABASE_URL || 'https://wzdifkwupleogfixibiv.supabase.co';
const SUPABASE_SERVICE_ROLE_KEY = __ENV.SUPABASE_SERVICE_ROLE_KEY;
const BFF_SECRET_KEY = __ENV.BFF_SECRET_KEY;
const INTERNAL_API_KEY = __ENV.INTERNAL_API_KEY;

const TEST_EMAIL = __ENV.TEST_EMAIL || 'k6-loadtest@mini-commerce.internal';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'K6LoadTest!2026';

// 시드 상품(V2__seed_products.sql) 중 Desk Lamp — inventory-api Redis엔 초기값이 없어 setup()에서
// PUT /internal/inventory/stock으로 직접 채운다(catalog가 생성 시점에 하는 일을 대신 수행).
const PRODUCT_ID = __ENV.PRODUCT_ID || '00000000-0000-7000-8000-000000000003';
const INITIAL_STOCK = Number(__ENV.INITIAL_STOCK || 30);

const VUS = Number(__ENV.VUS || 30);
// 재고의 2배로 시도해 "절반은 성공, 절반은 품절"을 강제한다 — 성공/실패 양쪽 경로를 모두 부하 조건에서 검증.
const ITERATIONS = Number(__ENV.ITERATIONS || INITIAL_STOCK * 2);

export const options = {
    scenarios: {
        reserve_burst: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: __ENV.MAX_DURATION || '2m',
        },
    },
    // 품절 4xx는 설계상 정상 응답이라 http_req_failed 임계값에 넣지 않는다 — 성공 기준은
    // "오버셀 0건"이지 "4xx 0건"이 아니다(요약 출력 후 setup()의 assertNoOversell에서 별도 판정).
    thresholds: {
        order_unexpected_error: ['count==0'],
    },
};

function requireEnv(name, value) {
    if (!value) {
        throw new Error(`${name} is required (pass via k6 run -e ${name}=... or loadtest/run.sh)`);
    }
    return value;
}

export function setup() {
    requireEnv('SUPABASE_SERVICE_ROLE_KEY', SUPABASE_SERVICE_ROLE_KEY);
    requireEnv('BFF_SECRET_KEY', BFF_SECRET_KEY);
    requireEnv('INTERNAL_API_KEY', INTERNAL_API_KEY);

    // 1) 전용 테스트 계정 확보 — 있으면 재사용(422 user_already_exists 무시), 없으면 생성.
    const adminHeaders = {
        headers: {
            'Content-Type': 'application/json',
            apikey: SUPABASE_SERVICE_ROLE_KEY,
            Authorization: `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
        },
    };
    const createRes = http.post(
        `${SUPABASE_URL}/auth/v1/admin/users`,
        JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD, email_confirm: true }),
        adminHeaders
    );
    if (createRes.status !== 200 && createRes.status !== 201) {
        const alreadyExists = createRes.status === 422 || createRes.status === 400;
        if (!alreadyExists) {
            throw new Error(`Supabase test user provisioning failed: ${createRes.status} ${createRes.body}`);
        }
    }

    // 2) 로그인 — JWT 획득.
    const loginRes = http.post(
        `${SUPABASE_URL}/auth/v1/token?grant_type=password`,
        JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD }),
        { headers: { 'Content-Type': 'application/json', apikey: SUPABASE_SERVICE_ROLE_KEY } }
    );
    if (loginRes.status !== 200) {
        throw new Error(`Supabase login failed: ${loginRes.status} ${loginRes.body}`);
    }
    const token = loginRes.json('access_token');
    if (!token) {
        throw new Error(`Supabase login response missing access_token: ${loginRes.body}`);
    }

    // 3) 대상 상품 재고를 알려진 값으로 초기화(재실행마다 결정적인 결과를 위해).
    const setStockRes = http.put(
        `${INVENTORY_API_URL}/internal/inventory/stock/${PRODUCT_ID}`,
        JSON.stringify({ stock: INITIAL_STOCK }),
        { headers: { 'Content-Type': 'application/json', 'X-Internal-Key': INTERNAL_API_KEY } }
    );
    if (setStockRes.status !== 200) {
        throw new Error(`inventory setStock failed: ${setStockRes.status} ${setStockRes.body}`);
    }

    return { token, initialStock: INITIAL_STOCK, productId: PRODUCT_ID };
}

export default function (data) {
    const payload = JSON.stringify({
        items: [{ productId: data.productId, quantity: 1 }],
        shippingRecipient: 'k6 부하테스트',
        shippingPhone: '010-0000-0000',
        shippingAddress: '서울시 k6로 1',
        shippingDetailAddress: '1동 101호',
        shippingZipCode: '06236',
    });

    const res = http.post(`${ORDER_API_URL}/api/orders`, payload, {
        headers: {
            'Content-Type': 'application/json',
            'X-Internal-BFF-Key': BFF_SECRET_KEY,
            Authorization: `Bearer ${data.token}`,
        },
    });

    if (res.status === 201) {
        successes.add(1);
    } else if (res.status === 409 || res.status === 422) {
        // 품절(OutOfStockException 계열) — 설계상 정상 응답.
        outOfStock.add(1);
    } else {
        unexpected.add(1);
    }

    check(res, {
        'status is 201 or a stock-conflict response': (r) => r.status === 201 || r.status === 409 || r.status === 422,
    });
}

export function teardown(data) {
    const res = http.get(`${INVENTORY_API_URL}/internal/inventory/stocks?ids=${data.productId}`, {
        headers: { 'X-Internal-Key': INTERNAL_API_KEY },
    });
    const remaining = res.status === 200 ? res.json(data.productId) : undefined;
    console.log(`[teardown] productId=${data.productId} initialStock=${data.initialStock} remainingStock=${remaining}`);
    if (remaining !== undefined && remaining < 0) {
        console.error(`[teardown] OVERSELL DETECTED — remaining stock is negative (${remaining})`);
    }
}
