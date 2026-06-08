package com.minicommerce.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // JWT 필터를 우회하기 위해 standaloneSetup 사용
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("성공: 주문 생성 시 201 Created와 OrderResponse JSON을 반환한다")
    void createOrder_ShouldReturn201WithOrderResponse() throws Exception {
        // given
        String customerId = "cust-1";
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.CreateOrderItemRequest("prod-1", 2L, null))
        );
        Order mockOrder = new Order("order-1", customerId, List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 2L, null)
        ));

        when(orderService.createOrder(any(CreateOrderRequest.class), eq(customerId)))
                .thenReturn(mockOrder);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        // authenticatedUserId 속성을 요청에 직접 주입 (JWT 필터 우회)
                        .requestAttr("authenticatedUserId", customerId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-1"))
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalAmount").value(20000));
    }

    @Test
    @DisplayName("성공: 결제 완료 처리 시 200 OK와 PAID 상태의 OrderResponse JSON을 반환한다")
    void completeFakePayment_ShouldReturn200WithPaidOrder() throws Exception {
        // given
        String orderId = "order-1";
        Order mockOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        mockOrder.markPaid(); // PENDING_PAYMENT → PAID 상태 전이

        when(orderService.completeFakePayment(orderId)).thenReturn(mockOrder);

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/complete-payment", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-1"))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("실패: items가 비어있는 주문 생성 시 400 Bad Request를 반환한다")
    void createOrder_EmptyItems_ShouldReturn400() throws Exception {
        // given - @NotEmpty 검증 실패 케이스
        String requestBody = """
                {
                    "items": []
                }
                """;

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isBadRequest());
    }
}
