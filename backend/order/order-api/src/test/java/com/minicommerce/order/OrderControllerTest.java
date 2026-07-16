package com.minicommerce.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minicommerce.order.adapter.in.web.CreateOrderRequest;
import com.minicommerce.order.adapter.in.web.OrderController;
import com.minicommerce.order.application.PlaceOrderCommand;
import com.minicommerce.order.application.port.in.ConfirmPaymentUseCase;
import com.minicommerce.order.application.port.in.GetOrdersUseCase;
import com.minicommerce.order.application.port.in.PlaceOrderUseCase;
import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderLineDraft;
import com.minicommerce.order.domain.exception.OrderAlreadyProcessedException;
import com.minicommerce.order.domain.exception.OrderNotFoundException;
import com.minicommerce.order.domain.exception.PaymentAmountMismatchException;
import com.minicommerce.global.ApiExceptionHandler;
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
    private PlaceOrderUseCase placeOrderUseCase;

    @Mock
    private ConfirmPaymentUseCase confirmPaymentUseCase;

    @Mock
    private GetOrdersUseCase getOrdersUseCase;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // JWT 필터를 우회하기 위해 standaloneSetup 사용. BusinessException→HTTP 상태 매핑 검증을 위해
        // ApiExceptionHandler를 ControllerAdvice로 등록한다.
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(placeOrderUseCase, confirmPaymentUseCase, getOrdersUseCase))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("성공: 주문 생성 시 201 Created와 OrderResponse JSON을 반환한다")
    void createOrder_ShouldReturn201WithOrderResponse() throws Exception {
        // given
        String customerId = "cust-1";
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.CreateOrderItemRequest("prod-1", 2L, null)),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );
        Order mockOrder = new Order("order-1", customerId, List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 2L, null)
        ));

        when(placeOrderUseCase.place(any(PlaceOrderCommand.class), eq(customerId)))
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
    @DisplayName("성공: 결제 승인 시 200 OK와 PAID 상태 + paymentKey를 담은 OrderResponse JSON을 반환한다")
    void confirmPayment_ShouldReturn200WithPaidOrder() throws Exception {
        // given
        String orderId = "order-1";
        Order mockOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        mockOrder.markPaid("pay-key-1"); // PENDING_PAYMENT → PAID 상태 전이

        when(confirmPaymentUseCase.confirm(eq(orderId), eq("cust-1"), eq("pay-key-1"), any(BigDecimal.class)))
                .thenReturn(mockOrder);

        String body = """
                {"paymentKey": "pay-key-1", "amount": 10000}
                """;

        // when & then
        mockMvc.perform(post("/api/orders/{orderId}/confirm-payment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-1"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentKey").value("pay-key-1"));
    }

    @Test
    @DisplayName("실패: 결제 금액이 주문 금액과 다르면 400 Bad Request를 반환한다")
    void confirmPayment_AmountMismatch_ShouldReturn400() throws Exception {
        String orderId = "order-1";
        when(confirmPaymentUseCase.confirm(eq(orderId), eq("cust-1"), eq("pay-key-1"), any(BigDecimal.class)))
                .thenThrow(new PaymentAmountMismatchException(orderId));

        String body = """
                {"paymentKey": "pay-key-1", "amount": 9999}
                """;

        mockMvc.perform(post("/api/orders/{orderId}/confirm-payment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패: 이미 처리된 주문에 결제 승인을 재요청하면 409 Conflict를 반환한다")
    void confirmPayment_AlreadyProcessed_ShouldReturn409() throws Exception {
        String orderId = "order-1";
        when(confirmPaymentUseCase.confirm(eq(orderId), eq("cust-1"), eq("pay-key-1"), any(BigDecimal.class)))
                .thenThrow(new OrderAlreadyProcessedException(orderId));

        String body = """
                {"paymentKey": "pay-key-1", "amount": 10000}
                """;

        mockMvc.perform(post("/api/orders/{orderId}/confirm-payment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr("authenticatedUserId", "cust-1"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("실패: 타인 주문에 결제 승인을 시도하면 404 Not Found를 반환한다(소유권 검증)")
    void confirmPayment_OtherUsersOrder_ShouldReturn404() throws Exception {
        String orderId = "order-1";
        when(confirmPaymentUseCase.confirm(eq(orderId), eq("attacker"), eq("pay-key-1"), any(BigDecimal.class)))
                .thenThrow(new OrderNotFoundException(orderId));

        String body = """
                {"paymentKey": "pay-key-1", "amount": 10000}
                """;

        mockMvc.perform(post("/api/orders/{orderId}/confirm-payment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .requestAttr("authenticatedUserId", "attacker"))
                .andExpect(status().isNotFound());
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
