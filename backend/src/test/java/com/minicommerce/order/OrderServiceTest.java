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
import com.minicommerce.order.domain.OrderLineDraft;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        when(productQueryPort.findProduct(productId)).thenThrow(new EntityNotFoundException("Product not found: " + productId));

        // when & then
        assertThatThrownBy(() -> orderService.place(command, "cust-1"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(inventoryPort).release(expectedHold);
        verify(orderRepository, never()).save(any(Order.class));
    }
}
