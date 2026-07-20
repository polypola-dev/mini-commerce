package com.minicommerce.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.minicommerce.inventory.InventoryService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private StockService stockService;

    @Test
    @DisplayName("availableStocks: 각 id를 default 0으로 조회하고 순서를 보존한다")
    void availableStocks_queries_each_id_with_default_zero() {
        when(inventoryService.availableStock("a", 0L)).thenReturn(5L);
        when(inventoryService.availableStock("b", 0L)).thenReturn(3L);

        Map<String, Long> result = stockService.availableStocks(List.of("a", "b"));

        assertThat(result).containsExactly(Map.entry("a", 5L), Map.entry("b", 3L));
    }

    @Test
    @DisplayName("availableStocks: null/blank id는 건너뛰고 null 목록은 빈 Map")
    void availableStocks_skips_null_and_blank_ids() {
        when(inventoryService.availableStock("a", 0L)).thenReturn(1L);

        assertThat(stockService.availableStocks(Arrays.asList("a", null, " "))).containsOnlyKeys("a");
        assertThat(stockService.availableStocks(null)).isEmpty();
    }

    @Test
    @DisplayName("setStock: 설정 후 갱신된 가용재고를 반환한다")
    void setStock_returns_updated_available_stock() {
        when(inventoryService.availableStock("p1", 50L)).thenReturn(50L);

        assertThat(stockService.setStock("p1", 50L)).isEqualTo(50L);

        verify(inventoryService).setStock("p1", 50L);
    }
}
