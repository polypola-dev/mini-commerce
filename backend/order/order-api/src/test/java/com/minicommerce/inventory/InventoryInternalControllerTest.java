package com.minicommerce.inventory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InventoryInternalControllerTest {

    @Mock
    private InventoryService inventoryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InventoryInternalController(inventoryService)).build();
    }

    @Test
    @DisplayName("GET /internal/inventory/stocks?ids=a,b: 각 id의 availableStock(default 0)을 Map으로 반환")
    void availableStocks_returns_map_for_each_id() throws Exception {
        when(inventoryService.availableStock("a", 0L)).thenReturn(5L);
        when(inventoryService.availableStock("b", 0L)).thenReturn(3L);

        mockMvc.perform(get("/internal/inventory/stocks").param("ids", "a,b")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.a").value(5))
                .andExpect(jsonPath("$.b").value(3));

        verify(inventoryService).availableStock("a", 0L);
        verify(inventoryService).availableStock("b", 0L);
    }

    @Test
    @DisplayName("GET /internal/inventory/stocks (ids 없음): 빈 Map 반환")
    void availableStocks_returns_empty_when_no_ids() throws Exception {
        mockMvc.perform(get("/internal/inventory/stocks").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    @DisplayName("PUT /internal/inventory/stock/{id}: setStock 호출 후 갱신된 재고 반환")
    void setStock_updates_and_returns_available_stock() throws Exception {
        when(inventoryService.availableStock("p1", 50L)).thenReturn(50L);

        mockMvc.perform(put("/internal/inventory/stock/p1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":50}"))
                .andExpect(status().isOk())
                .andExpect(content().string("50"));

        verify(inventoryService).setStock("p1", 50L);
    }

    @Test
    @DisplayName("POST /internal/inventory/stock/{id}/init: initializeStockIfAbsent 호출 후 최종 재고 반환")
    void initStock_initializes_and_returns_available_stock() throws Exception {
        when(inventoryService.availableStock("p1", 100L)).thenReturn(100L);

        mockMvc.perform(post("/internal/inventory/stock/p1/init")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaultStock\":100}"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));

        verify(inventoryService).initializeStockIfAbsent(eq("p1"), eq(100L));
    }
}
