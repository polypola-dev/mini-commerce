package com.minicommerce.order;

import com.minicommerce.order.application.OrderService;
import com.minicommerce.order.application.PlaceOrderCommand;
import com.minicommerce.order.application.port.out.InventoryPort;
import com.minicommerce.order.application.port.out.InventoryPort.ReservationState;
import com.minicommerce.order.application.port.out.InventoryPort.StockItem;
import com.minicommerce.order.application.port.out.OrderEventPublisher;
import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.application.port.out.PaymentGatewayPort;
import com.minicommerce.order.application.port.out.PaymentGatewayPort.Cancellation;
import com.minicommerce.order.application.port.out.PaymentGatewayPort.Confirmation;
import com.minicommerce.order.application.port.out.ProductQueryPort;
import com.minicommerce.order.application.port.out.ProductQueryPort.OptionInfo;
import com.minicommerce.order.application.port.out.ProductQueryPort.ProductInfo;
import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderLine;
import com.minicommerce.order.domain.OrderLineDraft;
import com.minicommerce.order.domain.OrderStatus;
import com.minicommerce.order.domain.exception.OrderAlreadyProcessedException;
import com.minicommerce.order.domain.exception.OrderCancelNotAllowedException;
import com.minicommerce.order.domain.exception.OrderNotFoundException;
import com.minicommerce.order.domain.exception.PaymentAmountMismatchException;
import com.minicommerce.order.domain.exception.ReservationNotActiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ProductQueryPort productQueryPort;
    @Mock
    private InventoryPort inventoryPort;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderEventPublisher eventPublisher;
    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(productQueryPort, inventoryPort, orderRepository, eventPublisher, paymentGatewayPort);
    }

    @Test
    @DisplayName("성공: 주문 생성 시 orderId 선생성 → 재고 예약(멱등 키=orderId) → 저장 → 이벤트 발행")
    void createOrder_Success() {
        // given
        String productId = "prod-1";
        PlaceOrderCommand command = new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.OrderItem(productId, 2L, null)),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );

        ProductInfo productInfo = new ProductInfo(productId, "테스트 상품", BigDecimal.valueOf(10000));

        when(productQueryPort.findProduct(productId)).thenReturn(productInfo);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Order order = orderService.place(command, "cust-1");

        // then — 저장된 주문의 id와 reserve에 넘긴 멱등 키(orderId)가 일치한다(GH #3 S3)
        assertThat(order).isNotNull();
        verify(inventoryPort).reserve(eq(order.getId()), eq(List.of(new StockItem(productId, 2L))));
        verify(orderRepository).save(any(Order.class));
        verify(inventoryPort, never()).release(any());
        verify(eventPublisher).publishOrderPlaced(eq(order.getId()), eq("cust-1"), any());
    }

    @Test
    @DisplayName("성공: 옵션이 지정되면 추가금액이 단가에 더해지고 OrderLine에 옵션값이 저장된다")
    void createOrder_withOption_addsAdditionalPrice() {
        // given
        String productId = "prod-1";
        String optionId = "option-1";
        PlaceOrderCommand command = new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.OrderItem(productId, 1L, optionId)),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );

        ProductInfo productInfo = new ProductInfo(productId, "테스트 상품", BigDecimal.valueOf(10000));
        OptionInfo optionInfo = new OptionInfo(BigDecimal.valueOf(5000), "화이트");

        when(productQueryPort.findProduct(productId)).thenReturn(productInfo);
        when(productQueryPort.findOption(optionId)).thenReturn(optionInfo);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Order order = orderService.place(command, "cust-1");

        // then: 단가 10000 + 추가금액 5000 = 15000
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(15000));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("실패: 상품이 존재하지 않으면 reserve 이전에 실패한다 — 보상할 예약이 없다(GH #3 S3 재배열)")
    void createOrder_ProductNotFound_failsBeforeReserve() {
        // given
        String productId = "prod-not-found";
        PlaceOrderCommand command = new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.OrderItem(productId, 2L, null)),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );

        when(productQueryPort.findProduct(productId)).thenThrow(new IllegalStateException("Product not found: " + productId));

        // when & then
        assertThatThrownBy(() -> orderService.place(command, "cust-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found");

        verify(inventoryPort, never()).reserve(any(), any());
        verify(inventoryPort, never()).release(any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("실패: 주문 저장이 실패하면 예약을 release(orderId)로 보상한다")
    void createOrder_SaveFails_compensatesWithRelease() {
        String productId = "prod-1";
        PlaceOrderCommand command = new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.OrderItem(productId, 1L, null)),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );
        when(productQueryPort.findProduct(productId))
                .thenReturn(new ProductInfo(productId, "테스트 상품", BigDecimal.valueOf(10000)));
        when(orderRepository.save(any(Order.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> orderService.place(command, "cust-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db down");

        // reserve에 쓴 orderId 그대로 release가 호출된다
        ArgumentCaptor<String> reservedOrderId = ArgumentCaptor.forClass(String.class);
        verify(inventoryPort).reserve(reservedOrderId.capture(), any());
        verify(inventoryPort).release(reservedOrderId.getValue());
        verify(eventPublisher, never()).publishOrderPlaced(any(), any(), any());
    }

    @Test
    @DisplayName("실패: 보상 release마저 실패해도 원 예외가 전파된다(리퍼 백스톱 전제)")
    void createOrder_ReleaseAlsoFails_originalExceptionPropagates() {
        String productId = "prod-1";
        PlaceOrderCommand command = new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.OrderItem(productId, 1L, null)),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );
        when(productQueryPort.findProduct(productId))
                .thenReturn(new ProductInfo(productId, "테스트 상품", BigDecimal.valueOf(10000)));
        when(orderRepository.save(any(Order.class))).thenThrow(new IllegalStateException("db down"));
        doThrow(new RuntimeException("inventory down")).when(inventoryPort).release(any());

        assertThatThrownBy(() -> orderService.place(command, "cust-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db down");
    }

    @Test
    @DisplayName("성공: 결제 승인 시 금액 대조 후 게이트웨이 승인 → PAID 저장 + paymentKey 기록 + 재고 확정 + OrderPaid 발행")
    void confirmPayment_Success() {
        // given
        Order pendingOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryPort.status("order-1")).thenReturn(ReservationState.RESERVED);
        when(paymentGatewayPort.confirm(eq("pay-key-1"), eq("order-1"), any(BigDecimal.class)))
                .thenReturn(new Confirmation("pay-key-1", "카드", Instant.now()));

        // when
        Order result = orderService.confirm("order-1", "cust-1", "pay-key-1", BigDecimal.valueOf(10000));

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getPaymentKey()).isEqualTo("pay-key-1");
        // 재고 확정은 동기 호출이 아니라 OrderPaid 발행으로 위임된다(GH #3 S4 코레오그래피).
        verify(eventPublisher).publishOrderPaid(eq("order-1"), eq("cust-1"), any());
    }

    @Test
    @DisplayName("실패: 결제 금액이 주문 금액과 다르면 게이트웨이 호출 전에 PaymentAmountMismatchException, 저장하지 않는다")
    void confirmPayment_AmountMismatch_throwsBeforeGateway() {
        Order pendingOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.confirm("order-1", "cust-1", "pay-key-1", BigDecimal.valueOf(9999)))
                .isInstanceOf(PaymentAmountMismatchException.class);

        verify(paymentGatewayPort, never()).confirm(any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: 이미 결제된 주문에 결제 승인을 재요청하면 OrderAlreadyProcessedException, 게이트웨이 호출·저장 없음")
    void confirmPayment_AlreadyProcessed_throws() {
        Order paidOrder = Order.reconstitute("order-1", "cust-1", OrderStatus.PAID, BigDecimal.valueOf(10000),
                Instant.now(), "old-key", null, null, null, null, null, List.<OrderLine>of());
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(paidOrder));

        assertThatThrownBy(() -> orderService.confirm("order-1", "cust-1", "pay-key-1", BigDecimal.valueOf(10000)))
                .isInstanceOf(OrderAlreadyProcessedException.class);

        verify(paymentGatewayPort, never()).confirm(any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: 타인 주문에 결제 승인을 시도하면 OrderNotFoundException, 게이트웨이 호출·저장 없음(소유권 검증)")
    void confirmPayment_OtherUsersOrder_throws() {
        Order pendingOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(pendingOrder));

        assertThatThrownBy(() -> orderService.confirm("order-1", "attacker", "pay-key-1", BigDecimal.valueOf(10000)))
                .isInstanceOf(OrderNotFoundException.class);

        verify(paymentGatewayPort, never()).confirm(any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: 예약이 이미 해제(RELEASED)된 주문은 PG 승인 전에 ReservationNotActiveException(만료 경합 가드)")
    void confirmPayment_ReservationReleased_rejectsBeforeGateway() {
        Order pendingOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(pendingOrder));
        when(inventoryPort.status("order-1")).thenReturn(ReservationState.RELEASED);

        assertThatThrownBy(() -> orderService.confirm("order-1", "cust-1", "pay-key-1", BigDecimal.valueOf(10000)))
                .isInstanceOf(ReservationNotActiveException.class);

        verify(paymentGatewayPort, never()).confirm(any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 주문을 결제 승인하면 OrderNotFoundException, 저장하지 않는다")
    void confirmPayment_OrderNotFound_throws() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.confirm("missing", "cust-1", "pay-key-1", BigDecimal.valueOf(10000)))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공: 만료 처리 시 PENDING_PAYMENT 주문이 EXPIRED로 저장된다")
    void expire_pendingOrder_marksExpired() {
        // given
        Order pendingOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(pendingOrder));

        // when
        orderService.expire("order-1");

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.EXPIRED);
    }

    @Test
    @DisplayName("이미 결제된 주문은 만료 처리해도 상태가 바뀌지 않는다(가드)")
    void expire_paidOrder_doesNotChangeStatus() {
        // given
        Order paidOrder = Order.reconstitute("order-1", "cust-1", OrderStatus.PAID, BigDecimal.valueOf(10000),
                Instant.now(), "pay-key-1", null, null, null, null, null, List.<OrderLine>of());
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(paidOrder));

        // when
        orderService.expire("order-1");

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("존재하지 않는 주문은 만료 처리를 조용히 무시한다(멱등)")
    void expire_orderNotFound_doesNothing() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        orderService.expire("missing");

        verify(orderRepository, never()).save(any());
    }

    private Order paidOrder() {
        return Order.reconstitute("order-1", "cust-1", OrderStatus.PAID, BigDecimal.valueOf(10000),
                Instant.now(), "pay-key-1", null, null, null, null, null, List.<OrderLine>of());
    }

    @Test
    @DisplayName("성공: 주문 취소 시 환불 → 재입고 → save → OrderCanceled 발행 순서로 처리되고 CANCELED가 된다")
    void cancel_Success() {
        Order order = paidOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGatewayPort.cancel("pay-key-1", "고객 변심")).thenReturn(new Cancellation(Instant.now()));

        Order result = orderService.cancel("order-1", "cust-1", "고객 변심");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
        // 재입고는 동기 호출이 아니라 OrderCanceled 발행으로 위임된다(GH #3 S4 코레오그래피).
        InOrder inOrder = inOrder(paymentGatewayPort, orderRepository, eventPublisher);
        inOrder.verify(paymentGatewayPort).cancel("pay-key-1", "고객 변심");
        inOrder.verify(orderRepository).save(any(Order.class));
        inOrder.verify(eventPublisher).publishOrderCanceled(eq("order-1"), eq("cust-1"), any());
    }

    @Test
    @DisplayName("실패: 타인 주문 취소 시도 → OrderNotFoundException, 환불/재입고/저장 없음")
    void cancel_OtherUsersOrder_throws() {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(paidOrder()));

        assertThatThrownBy(() -> orderService.cancel("order-1", "attacker", "고객 변심"))
                .isInstanceOf(OrderNotFoundException.class);

        verify(paymentGatewayPort, never()).cancel(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: PAID가 아닌 주문(PENDING_PAYMENT) 취소 → OrderCancelNotAllowedException, 환불/재입고/저장 없음")
    void cancel_NotPaid_throws() {
        Order pending = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> orderService.cancel("order-1", "cust-1", "고객 변심"))
                .isInstanceOf(OrderCancelNotAllowedException.class);

        verify(paymentGatewayPort, never()).cancel(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: 환불 예외 시 재입고/저장/이벤트 없이 전파(주문은 PAID 유지)")
    void cancel_RefundFails_propagatesWithoutLocalChanges() {
        Order order = paidOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(paymentGatewayPort.cancel(any(), any())).thenThrow(new RuntimeException("toss down"));

        assertThatThrownBy(() -> orderService.cancel("order-1", "cust-1", "고객 변심"))
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishOrderCanceled(any(), any(), any());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("실패: 환불 성공 후 로컬 저장 예외 시 이벤트 없이 전파(주문은 PAID 유지, 재시도 가능)")
    void cancel_LocalSaveFails_propagatesAndOrderStaysPaid() {
        Order order = paidOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(paymentGatewayPort.cancel(any(), any())).thenReturn(new Cancellation(Instant.now()));
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> orderService.cancel("order-1", "cust-1", "고객 변심"))
                .isInstanceOf(RuntimeException.class);

        verify(eventPublisher, never()).publishOrderCanceled(any(), any(), any());
    }

    @Test
    @DisplayName("성공: 관리자 취소 경로는 소유권을 검증하지 않고 취소한다(재입고는 OrderCanceled 발행으로 위임)")
    void cancelByAdmin_Success_noOwnershipCheck() {
        Order order = paidOrder();
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGatewayPort.cancel("pay-key-1", "관리자 취소")).thenReturn(new Cancellation(Instant.now()));

        Order result = orderService.cancelByAdmin("order-1", "관리자 취소");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELED);
        verify(eventPublisher).publishOrderCanceled(eq("order-1"), eq("cust-1"), any());
    }
}
