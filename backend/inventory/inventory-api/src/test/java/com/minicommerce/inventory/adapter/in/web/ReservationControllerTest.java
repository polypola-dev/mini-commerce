package com.minicommerce.inventory.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.minicommerce.inventory.ReservationStatus;
import com.minicommerce.inventory.application.port.in.GetReservationUseCase;
import com.minicommerce.inventory.application.port.in.ReleaseReservationUseCase;
import com.minicommerce.inventory.application.port.in.ReservationView;
import com.minicommerce.inventory.application.port.in.ReserveStockUseCase;
import com.minicommerce.inventory.application.port.in.ReserveStockUseCase.ReserveCommand;
import com.minicommerce.inventory.application.port.in.ReserveStockUseCase.ReserveResult;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 예약 POST의 본문 검증 계약. 누락 필드가 유즈케이스까지 내려가 NPE→500이 되던 회귀를 막는다 —
 * 호출자 잘못을 500으로 응답하면 RED 대시보드에 서버 장애로 잡히고, order-infra 어댑터가
 * "inventory 불가"로 오해해 무의미한 보상 경로를 태운다.
 */
@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReserveStockUseCase reserveStockUseCase;

    @Mock
    private ReleaseReservationUseCase releaseReservationUseCase;

    @Mock
    private GetReservationUseCase getReservationUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservationController(
                        reserveStockUseCase, releaseReservationUseCase, getReservationUseCase))
                .setControllerAdvice(new InventoryApiExceptionHandler())
                .build();
    }

    private org.springframework.test.web.servlet.ResultActions postBody(String json) throws Exception {
        return mockMvc.perform(post("/internal/inventory/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    @DisplayName("정상 본문: 201 Created + 유즈케이스에 커맨드 전달")
    void reserve_returns201_forValidBody() throws Exception {
        ReservationView view = new ReservationView(
                "res-1", "order-1", ReservationStatus.RESERVED.name(), Instant.parse("2026-07-21T00:00:00Z"));
        when(reserveStockUseCase.reserve(any(ReserveCommand.class)))
                .thenReturn(new ReserveResult(view, true));

        postBody("{\"orderId\":\"order-1\",\"items\":[{\"productId\":\"p1\",\"quantity\":2}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-1"));
    }

    @Test
    @DisplayName("빈 본문 {}: 400 — 이전에는 items가 null이라 NPE→500이었다")
    void reserve_returns400_forEmptyBody() throws Exception {
        postBody("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.type").value(
                        org.hamcrest.Matchers.containsString("invalid-request")));

        verify(reserveStockUseCase, never()).reserve(any());
    }

    @Test
    @DisplayName("items 빈 배열: 400")
    void reserve_returns400_forEmptyItems() throws Exception {
        postBody("{\"orderId\":\"order-1\",\"items\":[]}")
                .andExpect(status().isBadRequest());

        verify(reserveStockUseCase, never()).reserve(any());
    }

    @Test
    @DisplayName("orderId 공백: 400")
    void reserve_returns400_forBlankOrderId() throws Exception {
        postBody("{\"orderId\":\"  \",\"items\":[{\"productId\":\"p1\",\"quantity\":1}]}")
                .andExpect(status().isBadRequest());

        verify(reserveStockUseCase, never()).reserve(any());
    }

    @Test
    @DisplayName("quantity 0 이하: 400 — 음수 수량이 재고 로직에 도달하지 않는다")
    void reserve_returns400_forNonPositiveQuantity() throws Exception {
        postBody("{\"orderId\":\"order-1\",\"items\":[{\"productId\":\"p1\",\"quantity\":0}]}")
                .andExpect(status().isBadRequest());

        verify(reserveStockUseCase, never()).reserve(any());
    }
}
