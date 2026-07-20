package com.minicommerce.inventory.adapter.in.web;

import com.minicommerce.inventory.OutOfStockException;
import com.minicommerce.inventory.ReservationConflictException;
import com.minicommerce.inventory.ReservationStatus;
import jakarta.persistence.EntityNotFoundException;
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
 * 예약 사가 REST 경계의 에러 계약 검증(GH #3 S3). ProblemDetail type URI는 order-infra
 * InventoryRestAdapter가 도메인 예외 복원에 사용하는 계약이다.
 */
class InventoryApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/out-of-stock")
        void outOfStock() {
            throw new OutOfStockException("prod-1");
        }

        @GetMapping("/test/reservation-conflict")
        void reservationConflict() {
            throw new ReservationConflictException("order-1", ReservationStatus.RELEASED);
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new EntityNotFoundException("Reservation not found for order: order-1");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new InventoryApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("OutOfStockException → 409 Conflict, title 'Out of stock', type 'out-of-stock' 포함")
    void outOfStockException_returns409WithProblemDetail() throws Exception {
        mockMvc.perform(get("/test/out-of-stock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Out of stock"))
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.containsString("out-of-stock")));
    }

    @Test
    @DisplayName("ReservationConflictException → 409 Conflict, type 'reservation-conflict'")
    void reservationConflict_returns409() throws Exception {
        mockMvc.perform(get("/test/reservation-conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.containsString("reservation-conflict")));
    }

    @Test
    @DisplayName("EntityNotFoundException → 404 Not Found, type 'reservation-not-found'")
    void entityNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.containsString("reservation-not-found")));
    }
}
