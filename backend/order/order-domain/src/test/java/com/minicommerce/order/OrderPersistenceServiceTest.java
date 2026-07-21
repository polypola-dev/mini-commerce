package com.minicommerce.order;

import com.minicommerce.order.application.OrderPersistenceService;
import com.minicommerce.order.application.port.out.OrderEventPublisher;
import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderLine;
import com.minicommerce.order.domain.OrderLineDraft;
import com.minicommerce.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 저장 → 아웃박스 이벤트 발행 순서와 인자 전달만 검증한다(GH #21). 원격 호출 배치·
 * 트랜잭션 경계는 OrderServiceTest(모킹된 협력 빈)와 실제 통합 경로(compose e2e)가 담당한다.
 */
@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderEventPublisher eventPublisher;

    private OrderPersistenceService orderPersistenceService;

    @BeforeEach
    void setUp() {
        orderPersistenceService = new OrderPersistenceService(orderRepository, eventPublisher);
    }

    private Order newOrder() {
        return new Order("order-1", "cust-1", List.of(
                new OrderLineDraft("prod-1", "테스트 상품", BigDecimal.valueOf(10000), 1L, null)
        ));
    }

    private Order paidOrder() {
        return Order.reconstitute("order-1", "cust-1", OrderStatus.PAID, BigDecimal.valueOf(10000),
                Instant.now(), "pay-key-1", null, null, null, null, null, List.<OrderLine>of());
    }

    @Test
    @DisplayName("persistPlacedOrder: 저장 후 OrderPlaced를 저장된 값으로 발행하고 저장 결과를 반환한다")
    void persistPlacedOrder_savesThenPublishes() {
        Order order = newOrder();
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderPersistenceService.persistPlacedOrder(order);

        assertThat(result).isSameAs(order);
        InOrder inOrder = inOrder(orderRepository, eventPublisher);
        inOrder.verify(orderRepository).save(order);
        inOrder.verify(eventPublisher).publishOrderPlaced(eq("order-1"), eq("cust-1"), any(BigDecimal.class));
    }

    @Test
    @DisplayName("persistConfirmedPayment: 저장 후 OrderPaid를 저장된 값으로 발행하고 저장 결과를 반환한다")
    void persistConfirmedPayment_savesThenPublishes() {
        Order order = paidOrder();
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderPersistenceService.persistConfirmedPayment(order);

        assertThat(result).isSameAs(order);
        InOrder inOrder = inOrder(orderRepository, eventPublisher);
        inOrder.verify(orderRepository).save(order);
        inOrder.verify(eventPublisher).publishOrderPaid(eq("order-1"), eq("cust-1"), any(BigDecimal.class));
    }

    @Test
    @DisplayName("persistCanceledOrder: 저장 후 OrderCanceled를 저장된 값으로 발행하고 저장 결과를 반환한다")
    void persistCanceledOrder_savesThenPublishes() {
        Order order = paidOrder();
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderPersistenceService.persistCanceledOrder(order);

        assertThat(result).isSameAs(order);
        InOrder inOrder = inOrder(orderRepository, eventPublisher);
        inOrder.verify(orderRepository).save(order);
        inOrder.verify(eventPublisher).publishOrderCanceled(eq("order-1"), eq("cust-1"), any(BigDecimal.class));
    }
}
