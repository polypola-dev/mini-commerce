package com.minicommerce.order.adapter.out.inventory;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.minicommerce.global.InternalApiContract;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 서비스간 인증 헤더(B3, ADR-020) 부착 검증. 세 REST 간선 중 가장 민감한 경로(예약 사가 —
 * 재고를 실제로 변경한다)라 헤더 누락이 조용히 지나가지 않게 설정 클래스를 직접 검증한다.
 *
 * <p>이 설정은 타임아웃 때문에 {@code requestFactory}를 직접 지정하므로 Builder에 목을 바인딩하는
 * 통상 방식이 덮어써진다 — 완성된 클라이언트를 {@code mutate()}해 기본 헤더는 보존한 채 요청
 * 팩토리만 목으로 되돌린다.
 */
class InventoryRestClientConfigTest {

    @Test
    @DisplayName("inventorySagaRestClient: 모든 요청에 X-Internal-Key를 붙인다")
    void attaches_internal_key_header() {
        RestClient configured = new InventoryRestClientConfig()
                .inventorySagaRestClient(RestClient.builder(), "http://inventory.internal", "test-internal-key");

        RestClient.Builder mutated = configured.mutate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(mutated).build();

        server.expect(requestTo("http://inventory.internal/internal/inventory/reservations"))
                .andExpect(header(InternalApiContract.INTERNAL_KEY_HEADER, "test-internal-key"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        mutated.build().post().uri("/internal/inventory/reservations").retrieve().toBodilessEntity();

        server.verify();
    }
}
