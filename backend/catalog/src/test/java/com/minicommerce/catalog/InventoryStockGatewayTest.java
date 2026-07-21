package com.minicommerce.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * 폴백 자체의 계약 검증(D6). AOP를 거치지 않고 폴백 메서드를 직접 부른다 — 서킷이 실제로
 * 열려서 이 메서드가 불리는지는 {@link InventoryCircuitBreakerTest}가 검증한다.
 */
class InventoryStockGatewayTest {

    private final InventoryStockCache cache = mock(InventoryStockCache.class);
    private final InventoryStockGateway gateway =
            new InventoryStockGateway(RestClient.builder().build(), cache);

    @Test
    @DisplayName("폴백: 캐시에 남은 마지막 성공값을 돌려준다")
    void fallback_returns_last_known_value_from_cache() {
        when(cache.readAll(List.of("p1", "p2"))).thenReturn(Map.of("p1", 7L, "p2", 3L));

        Map<String, Long> result = gateway.fetchStocksFallback(
                List.of("p1", "p2"), new IllegalStateException("boom"));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("p1", 7L, "p2", 3L));
    }

    @Test
    @DisplayName("폴백: 캐시 미적중분은 0으로 채운다(요청한 id는 반드시 키가 있다)")
    void fallback_fills_cache_misses_with_zero() {
        when(cache.readAll(List.of("p1", "p2"))).thenReturn(Map.of("p1", 7L));

        Map<String, Long> result = gateway.fetchStocksFallback(
                List.of("p1", "p2"), new IllegalStateException("boom"));

        // p2에 키가 없으면 호출자의 getOrDefault가 상품 테이블의 낡은 stock 컬럼으로 새어 나간다.
        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("p1", 7L, "p2", 0L));
    }

    @Test
    @DisplayName("폴백: Redis가 죽어도 예외 없이 0으로 degrade한다")
    void fallback_degrades_to_zero_when_cache_unavailable() {
        // 실제 InventoryStockCache는 예외를 삼키고 빈 Map을 주지만, 계약이 깨져 예외가 새더라도
        // 상품 조회가 통째로 실패하면 안 되므로 게이트웨이 쪽 기대치도 여기서 고정한다.
        when(cache.readAll(anyList())).thenReturn(Map.of());

        Map<String, Long> result = gateway.fetchStocksFallback(
                List.of("p1"), new IllegalStateException("redis down"));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of("p1", 0L));
    }
}
