package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.domain.exception.InventoryUnavailableException;
import com.minicommerce.order.domain.exception.OutOfStockException;
import com.minicommerce.order.domain.exception.ReservationNotActiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 재고 사가 도메인 예외의 ProblemDetail 매핑 검증(GH #3 S3). out-of-stock 409 계약은
 * in-process 시절 응답과 동일해야 한다(프론트가 type으로 품절을 분기).
 */
class InventorySagaExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/out-of-stock")
        void outOfStock() {
            throw new OutOfStockException("Out of stock for order order-1");
        }

        @GetMapping("/test/reservation-not-active")
        void reservationNotActive() {
            throw new ReservationNotActiveException("order-1", "RELEASED");
        }

        @GetMapping("/test/inventory-unavailable")
        void inventoryUnavailable() {
            throw new InventoryUnavailableException("Inventory reserve failed", null);
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new InventorySagaExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("OutOfStockException → 409 Conflict, title 'Out of stock', type 'out-of-stock' 포함(기존 계약 보존)")
    void outOfStockException_returns409WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test/out-of-stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Out of stock"))
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.containsString("out-of-stock")));
    }

    @Test
    @DisplayName("ReservationNotActiveException → 409 Conflict, type 'reservation-not-active'")
    void reservationNotActive_returns409() throws Exception {
        mockMvc.perform(get("/test/reservation-not-active"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.containsString("reservation-not-active")));
    }

    @Test
    @DisplayName("InventoryUnavailableException → 503 Service Unavailable, type 'inventory-unavailable'")
    void inventoryUnavailable_returns503() throws Exception {
        mockMvc.perform(get("/test/inventory-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.containsString("inventory-unavailable")));
    }
}
