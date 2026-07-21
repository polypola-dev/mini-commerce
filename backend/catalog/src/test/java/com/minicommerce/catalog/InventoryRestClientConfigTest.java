package com.minicommerce.catalog;

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
 * 서비스간 인증 헤더(B3, ADR-020) 부착 검증. {@link InventoryClientTest}는 자체 Builder를 쓰기
 * 때문에 이 {@code @Configuration}을 지나가지 않는다 — 헤더 누락이 런타임에만 드러나는 공백을
 * 막으려고 설정 클래스를 직접 호출해 검증한다.
 */
class InventoryRestClientConfigTest {

    @Test
    @DisplayName("inventoryRestClient: 모든 요청에 X-Internal-Key를 붙인다")
    void attaches_internal_key_header() {
        RestClient configured = new InventoryRestClientConfig()
                .inventoryRestClient(RestClient.builder(), "http://inventory.internal", "test-internal-key");

        // 설정이 만든 클라이언트의 기본 헤더를 보존한 채 요청 팩토리만 목으로 교체한다.
        RestClient.Builder mutated = configured.mutate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(mutated).build();

        server.expect(requestTo("http://inventory.internal/internal/inventory/stocks"))
                .andExpect(header(InternalApiContract.INTERNAL_KEY_HEADER, "test-internal-key"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        mutated.build().get().uri("/internal/inventory/stocks").retrieve().toBodilessEntity();

        server.verify();
    }
}
