package com.minicommerce.order.application;

import com.minicommerce.order.application.port.out.OrderEventPublisher;
import com.minicommerce.order.application.port.out.OrderNumberPort;
import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.domain.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 저장 + 아웃박스 이벤트 발행 전용 협력 빈(GH #21). {@link OrderService}의 유즈케이스 메서드에서
 * inventory-api/PG 원격 호출을 제거한 뒤 호출하는 마지막 단계만 담당한다.
 *
 * <p>이 클래스에만 {@code @Transactional}을 걸어 로컬 DB 트랜잭션 범위를 "저장 + 이벤트 발행"으로
 * 좁힌다 — 원격 호출이 트랜잭션(=Hikari 커넥션 점유) 안에 있으면 inventory-api/PG 지연이 곧바로
 * 커넥션 풀 고갈로 번져 무관한 조회 요청까지 막힐 수 있다. {@code OrderService}가 자기 자신을
 * 호출하는 방식(같은 빈 안에서 private 메서드 분리)으로는 Spring AOP 프록시를 안 타 트랜잭션이
 * 걸리지 않으므로, 반드시 별도 빈으로 분리해야 한다.
 */
// OrderService/OrderServiceTest와 동일한 이유로 public(테스트가 com.minicommerce.order
// 패키지에 있어 package-private이면 컴파일이 안 됨).
@Service
@Transactional
public class OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final OrderNumberPort orderNumberPort;

    public OrderPersistenceService(OrderRepository orderRepository, OrderEventPublisher eventPublisher,
                                   OrderNumberPort orderNumberPort) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.orderNumberPort = orderNumberPort;
    }

    public Order persistPlacedOrder(Order order) {
        // 표시 전용 주문번호는 이 트랜잭션 안에서 채번한다(GH #19) — 채번(카운터 증가)과 주문 저장이
        // 한 트랜잭션이라 저장 실패 시 번호도 함께 롤백돼 일련번호에 구멍이 나지 않는다.
        order.assignOrderNumber(orderNumberPort.generate(order.getCreatedAt()));
        Order saved = orderRepository.save(order);
        eventPublisher.publishOrderPlaced(saved.getId(), saved.getOrderNumber(), saved.getCustomerId(), saved.getTotalAmount());
        return saved;
    }

    public Order persistConfirmedPayment(Order order) {
        Order saved = orderRepository.save(order);
        eventPublisher.publishOrderPaid(saved.getId(), saved.getOrderNumber(), saved.getCustomerId(), saved.getTotalAmount());
        return saved;
    }

    public Order persistCanceledOrder(Order order) {
        Order saved = orderRepository.save(order);
        eventPublisher.publishOrderCanceled(saved.getId(), saved.getOrderNumber(), saved.getCustomerId(), saved.getTotalAmount());
        return saved;
    }
}
