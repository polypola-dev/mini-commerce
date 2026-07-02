package com.minicommerce.order;

import com.minicommerce.order.application.OrderService;
import com.minicommerce.order.application.PlaceOrderCommand;
import com.minicommerce.order.application.port.out.InventoryPort;
import com.minicommerce.order.application.port.out.InventoryPort.StockHold;
import com.minicommerce.order.application.port.out.InventoryPort.StockItem;
import com.minicommerce.order.application.port.out.OrderEventPublisher;
import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.application.port.out.ProductQueryPort;
import com.minicommerce.order.application.port.out.ProductQueryPort.OptionInfo;
import com.minicommerce.order.application.port.out.ProductQueryPort.ProductInfo;
import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderLine;
import com.minicommerce.order.domain.OrderLineDraft;
import com.minicommerce.order.domain.OrderStatus;
import com.minicommerce.order.domain.exception.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(productQueryPort, inventoryPort, orderRepository, eventPublisher);
    }

    @Test
    @DisplayName("성공: 주문 생성 시 Redis 재고가 예약되고 DB에 주문 및 예약 정보가 정상 저장된다")
    void createOrder_Success() {
        // given
        String productId = "prod-1";
        PlaceOrderCommand command = new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.OrderItem(productId, 2L, null)),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );

        StockHold expectedHold = new StockHold(
                "res-1",
                Instant.now().plusSeconds(600),
                List.of(new StockItem(productId, 2L))
        );

        ProductInfo productInfo = new ProductInfo(productId, "테스트 상품", BigDecimal.valueOf(10000));
        Order mockSavedOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft(productId, "테스트 상품", BigDecimal.valueOf(10000), 2L, null)
        ));

        when(inventoryPort.reserve(any())).thenReturn(expectedHold);
        when(productQueryPort.findProduct(productId)).thenReturn(productInfo);
        when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

        // when
        Order order = orderService.place(command, "cust-1");

        // then
        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo("order-1");

        verify(inventoryPort).reserve(any());
        verify(orderRepository).save(any(Order.class));
        verify(inventoryPort).createReservationForOrder(eq("order-1"), eq(expectedHold));
        verify(inventoryPort, never()).release(any());
        verify(eventPublisher).publishOrderPlaced(any(), any(), any());
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

        StockHold expectedHold = new StockHold(
                "res-1",
                Instant.now().plusSeconds(600),
                List.of(new StockItem(productId, 1L))
        );

        ProductInfo productInfo = new ProductInfo(productId, "테스트 상품", BigDecimal.valueOf(10000));
        OptionInfo optionInfo = new OptionInfo(BigDecimal.valueOf(5000), "화이트");

        when(inventoryPort.reserve(any())).thenReturn(expectedHold);
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
    @DisplayName("실패: 상품이 존재하지 않으면 주문 생성이 실패하고 예약된 Redis 재고를 롤백(release)한다")
    void createOrder_ProductNotFound_ShouldRollbackRedisInventory() {
        // given
        String productId = "prod-not-found";
        PlaceOrderCommand command = new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.OrderItem(productId, 2L, null)),
                "받는사람", "010-0000-0000", "서울시 강남구", "101동 101호", "12345"
        );

        StockHold expectedHold = new StockHold(
                "res-1",
                Instant.now().plusSeconds(600),
                List.of(new StockItem(productId, 2L))
        );

        when(inventoryPort.reserve(any())).thenReturn(expectedHold);
        when(productQueryPort.findProduct(productId)).thenThrow(new IllegalStateException("Product not found: " + productId));

        // when & then
        assertThatThrownBy(() -> orderService.place(command, "cust-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found");

        verify(inventoryPort).release(expectedHold);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("성공: 결제 완료 시 주문 상태가 PAID로 저장되고 재고 확정 후 OrderPaid 이벤트가 발행된다")
    void completePayment_Success() {
        // given
        Order pendingOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Order result = orderService.complete("order-1");

        // then
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(inventoryPort).confirmByOrderId("order-1");
        verify(eventPublisher).publishOrderPaid(eq("order-1"), eq("cust-1"), any());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 주문을 결제 완료하면 OrderNotFoundException, 저장하지 않는다")
    void completePayment_OrderNotFound_throws() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.complete("missing"))
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
                Instant.now(), null, null, null, null, null, List.<OrderLine>of());
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
}
