package com.minicommerce.inventory.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.minicommerce.inventory.application.port.in.GetStocksUseCase;
import com.minicommerce.inventory.application.port.in.SetStockUseCase;

@ExtendWith(MockitoExtension.class)
class InventoryInternalControllerTest {

    @Mock
    private GetStocksUseCase getStocksUseCase;

    @Mock
    private SetStockUseCase setStockUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new InventoryInternalController(getStocksUseCase, setStockUseCase))
                .build();
    }

    @Test
    @DisplayName("GET /internal/inventory/stocks?ids=a,b: 유즈케이스 결과 Map을 그대로 반환")
    void availableStocks_returns_map_for_each_id() throws Exception {
        when(getStocksUseCase.availableStocks(List.of("a", "b"))).thenReturn(Map.of("a", 5L, "b", 3L));

        mockMvc.perform(get("/internal/inventory/stocks").param("ids", "a,b")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.a").value(5))
                .andExpect(jsonPath("$.b").value(3));
    }

    @Test
    @DisplayName("GET /internal/inventory/stocks (ids 없음): 빈 Map 반환")
    void availableStocks_returns_empty_when_no_ids() throws Exception {
        when(getStocksUseCase.availableStocks(null)).thenReturn(Map.of());

        mockMvc.perform(get("/internal/inventory/stocks").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    @DisplayName("PUT /internal/inventory/stock/{id}: setStock 호출 후 갱신된 재고 반환")
    void setStock_updates_and_returns_available_stock() throws Exception {
        when(setStockUseCase.setStock("p1", 50L)).thenReturn(50L);

        mockMvc.perform(put("/internal/inventory/stock/p1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":50}"))
                .andExpect(status().isOk())
                .andExpect(content().string("50"));

        verify(setStockUseCase).setStock("p1", 50L);
    }
}
