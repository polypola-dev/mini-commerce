package com.minicommerce.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * 정상 경로(서킷 닫힘) 계약 검증. 서킷 전이·폴백은 {@link InventoryStockGatewayTest}와
 * {@link InventoryCircuitBreakerTest}가 다룬다.
 */
class InventoryClientTest {

    private MockRestServiceServer server;
    private InventoryStockCache cache;
    private InventoryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://inventory.internal");
        server = MockRestServiceServer.bindTo(builder).build();
        cache = mock(InventoryStockCache.class);
        when(cache.readAll(anyList())).thenReturn(Map.of());
        client = new InventoryClient(new InventoryStockGateway(builder.build(), cache));
    }

    @Test
    void availableStocks_returns_map_from_batch_endpoint() {
        server.expect(requestTo("http://inventory.internal/internal/inventory/stocks?ids=p1&ids=p2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"p1\":8,\"p2\":3}", MediaType.APPLICATION_JSON));

        Map<String, Long> result = client.availableStocks(List.of("p1", "p2"));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("p1", 8L, "p2", 3L));
        server.verify();
    }

    @Test
    void availableStocks_writes_successful_result_to_fallback_cache() {
        server.expect(requestTo("http://inventory.internal/internal/inventory/stocks?ids=p1"))
                .andRespond(withSuccess("{\"p1\":8}", MediaType.APPLICATION_JSON));

        client.availableStocks(List.of("p1"));

        // 다음 장애 때 돌려줄 "마지막 성공값"이 여기서 만들어진다 — 이게 빠지면 폴백이 항상 0이다.
        verify(cache).writeAll(Map.of("p1", 8L));
    }

    @Test
    void availableStocks_returns_empty_without_calling_server_when_ids_empty() {
        Map<String, Long> result = client.availableStocks(List.of());

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void availableStock_single_delegates_to_batch_endpoint() {
        server.expect(requestTo("http://inventory.internal/internal/inventory/stocks?ids=p1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"p1\":8}", MediaType.APPLICATION_JSON));

        long result = client.availableStock("p1", 99L);

        assertThat(result).isEqualTo(8L);
        server.verify();
    }

    @Test
    void availableStock_single_falls_back_to_default_when_absent_from_response() {
        server.expect(requestTo("http://inventory.internal/internal/inventory/stocks?ids=p1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        long result = client.availableStock("p1", 99L);

        assertThat(result).isEqualTo(99L);
        server.verify();
    }

    @Test
    void setStock_sends_put_with_stock_body() {
        server.expect(requestTo("http://inventory.internal/internal/inventory/stock/p1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"stock\":50}"))
                .andRespond(withSuccess("50", MediaType.APPLICATION_JSON));

        client.setStock("p1", 50L);

        server.verify();
        verify(cache).write("p1", 50L);
    }
}
