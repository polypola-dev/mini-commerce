package com.minicommerce.order.adapter.out.catalog;

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
 * 서비스간 인증 헤더(B3, ADR-020) 부착 검증. {@link CatalogProductAdapterTest}는 자체 Builder를
 * 쓰기 때문에 이 {@code @Configuration}을 지나가지 않는다.
 */
class CatalogRestClientConfigTest {

    @Test
    @DisplayName("catalogRestClient: 모든 요청에 X-Internal-Key를 붙인다")
    void attaches_internal_key_header() {
        RestClient configured = new CatalogRestClientConfig()
                .catalogRestClient(RestClient.builder(), "http://catalog.internal", "test-internal-key");

        RestClient.Builder mutated = configured.mutate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(mutated).build();

        server.expect(requestTo("http://catalog.internal/internal/products/p1"))
                .andExpect(header(InternalApiContract.INTERNAL_KEY_HEADER, "test-internal-key"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        mutated.build().get().uri("/internal/products/p1").retrieve().toBodilessEntity();

        server.verify();
    }
}
