package com.minicommerce.order;

import com.minicommerce.catalog.Product;
import com.minicommerce.catalog.ProductRepository;
import com.minicommerce.inventory.InventoryHold;
import com.minicommerce.inventory.InventoryItem;
import com.minicommerce.inventory.InventoryReservation;
import com.minicommerce.inventory.InventoryReservationRepository;
import com.minicommerce.inventory.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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
    private ProductRepository productRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryReservationRepository reservationRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                productRepository,
                orderRepository,
                reservationRepository,
                inventoryService,
                transactionTemplate,
                eventPublisher
        );

        // TransactionTemplate이 들어오면 트랜잭션 콜백을 즉시 실행하도록 Mock 설정
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    @DisplayName("성공: 주문 생성 시 Redis 재고가 예약되고 DB에 주문 및 예약 정보가 정상 저장된다")
    void createOrder_Success() {
        // given
        String productId = "prod-1";
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.CreateOrderItemRequest(productId, 2L))
        );

        InventoryHold expectedHold = new InventoryHold(
                "res-1",
                Instant.now().plusSeconds(600),
                List.of(new InventoryItem(productId, 2L))
        );

        Product expectedProduct = new Product(productId, "테스트 상품", "상품 설명", BigDecimal.valueOf(10000), 10L, "imageUrl");

        Order mockSavedOrder = new Order("order-1", "cust-1", List.of(
                new OrderLineDraft(productId, "테스트 상품", BigDecimal.valueOf(10000), 2L)
        ));

        when(inventoryService.reserve(any())).thenReturn(expectedHold);
        when(productRepository.findById(productId)).thenReturn(Optional.of(expectedProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(mockSavedOrder);

        // when
        Order order = orderService.createOrder(request, "cust-1");

        // then
        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo("order-1");
        
        verify(inventoryService).reserve(any());
        verify(orderRepository).save(any(Order.class));
        verify(reservationRepository).save(any(InventoryReservation.class));
        verify(inventoryService, never()).release(any());
        verify(eventPublisher).publishEvent(any(OrderPlacedEvent.class));
    }

    @Test
    @DisplayName("실패: 상품이 존재하지 않으면 주문 생성 트랜잭션이 실패하고 예약된 Redis 재고를 롤백(release)한다")
    void createOrder_ProductNotFound_ShouldRollbackRedisInventory() {
        // given
        String productId = "prod-not-found";
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new CreateOrderRequest.CreateOrderItemRequest(productId, 2L))
        );

        InventoryHold expectedHold = new InventoryHold(
                "res-1",
                Instant.now().plusSeconds(600),
                List.of(new InventoryItem(productId, 2L))
        );

        when(inventoryService.reserve(any())).thenReturn(expectedHold);
        when(productRepository.findById(productId)).thenReturn(Optional.empty()); // 상품 없음

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request, "cust-1"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");

        // DB 트랜잭션이 비정상 종료되었으므로, 확보했던 Redis 캐시 재고가 반드시 롤백(release) 처리되어야 함
        verify(inventoryService).release(expectedHold);
        verify(orderRepository, never()).save(any(Order.class));
    }
}
