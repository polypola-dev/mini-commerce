package com.minicommerce.order.application;

import com.minicommerce.order.application.port.in.CancelOrderUseCase;
import com.minicommerce.order.application.port.in.ConfirmPaymentUseCase;
import com.minicommerce.order.application.port.in.ExpireOrderUseCase;
import com.minicommerce.order.application.port.in.GetOrdersUseCase;
import com.minicommerce.order.application.port.in.PlaceOrderUseCase;
import com.minicommerce.order.application.port.out.InventoryPort;
import com.minicommerce.order.application.port.out.InventoryPort.StockItem;
import com.minicommerce.order.application.port.out.OrderEventPublisher;
import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.application.port.out.PaymentGatewayPort;
import com.minicommerce.order.application.port.out.PaymentGatewayPort.Confirmation;
import com.minicommerce.order.application.port.out.ProductQueryPort;
import com.minicommerce.order.application.port.out.ProductQueryPort.OptionInfo;
import com.minicommerce.order.application.port.out.ProductQueryPort.ProductInfo;
import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderLineDraft;
import com.minicommerce.order.domain.OrderStatus;
import com.minicommerce.order.domain.exception.OrderAlreadyProcessedException;
import com.minicommerce.order.domain.exception.OrderCancelNotAllowedException;
import com.minicommerce.order.domain.exception.OrderNotFoundException;
import com.minicommerce.order.domain.exception.PaymentAmountMismatchException;
import com.minicommerce.order.domain.exception.ReservationNotActiveException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
// order-domain은 최소 의존(spring-context/tx)이라 slf4j가 클래스패스에 없다 — spring-jcl 경유
// commons-logging이 이 모듈에서 쓸 수 있는 로깅 파사드다(런타임엔 SLF4J로 브리지됨).
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService implements PlaceOrderUseCase, ConfirmPaymentUseCase, CancelOrderUseCase, GetOrdersUseCase, ExpireOrderUseCase {

    private static final Log log = LogFactory.getLog(OrderService.class);

    private final ProductQueryPort productQueryPort;
    private final InventoryPort inventoryPort;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final PaymentGatewayPort paymentGatewayPort;

    public OrderService(
            ProductQueryPort productQueryPort,
            InventoryPort inventoryPort,
            OrderRepository orderRepository,
            OrderEventPublisher eventPublisher,
            PaymentGatewayPort paymentGatewayPort
    ) {
        this.productQueryPort = productQueryPort;
        this.inventoryPort = inventoryPort;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.paymentGatewayPort = paymentGatewayPort;
    }

    @Override
    public Order place(PlaceOrderCommand command, String customerId) {
        // 상품/옵션 조회(읽기 전용)를 reserve보다 앞에 둔다(GH #3 S3) — 실패해도 보상할 것이 없다.
        List<OrderLineDraft> lines = command.items().stream()
                .map(item -> {
                    ProductInfo product = productQueryPort.findProduct(item.productId());
                    BigDecimal unitPrice = product.price();
                    String optionValue = null;
                    if (item.selectedOptionId() != null && !item.selectedOptionId().isBlank()) {
                        OptionInfo option = productQueryPort.findOption(item.selectedOptionId());
                        unitPrice = unitPrice.add(option.additionalPrice());
                        optionValue = option.optionValue();
                    }
                    return new OrderLineDraft(item.productId(), product.name(), unitPrice, item.quantity(), optionValue);
                })
                .toList();

        // orderId를 선생성해 예약 멱등 키로 사용한다(예약 ID = orderId, GH #3 S3). 원격 reserve가
        // 타임아웃 후 재시도돼도 inventory 쪽에서 이중 차감 없이 수렴한다.
        String orderId = UUID.randomUUID().toString();
        List<StockItem> stockItems = command.items().stream()
                .map(i -> new StockItem(i.productId(), i.quantity()))
                .toList();
        inventoryPort.reserve(orderId, stockItems);

        try {
            Order order = orderRepository.save(new Order(
                    orderId, customerId, lines,
                    command.shippingRecipient(), command.shippingPhone(),
                    command.shippingAddress(), command.shippingDetailAddress(), command.shippingZipCode()
            ));
            eventPublisher.publishOrderPlaced(order.getId(), order.getCustomerId(), order.getTotalAmount());
            return order;
        } catch (RuntimeException e) {
            try {
                inventoryPort.release(orderId);
            } catch (RuntimeException releaseFailure) {
                // 보상 자체가 실패해도 원장(RESERVED + expires_at)이 남아 있어 리퍼가 만료 시점에
                // 백스톱으로 해제한다 — 원 예외를 삼키지 않는다.
                log.warn("Reserve compensation failed for orderId=" + orderId
                        + " — reaper will release on expiry", releaseFailure);
            }
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrders(String customerId) {
        return orderRepository.findAllByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getCustomerId().equals(customerId)) {
            throw new OrderNotFoundException(orderId);
        }
        return order;
    }

    @Override
    public Order confirm(String orderId, String customerId, String paymentKey, BigDecimal amount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getCustomerId().equals(customerId)) {
            throw new OrderNotFoundException(orderId);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new OrderAlreadyProcessedException(orderId);
        }
        if (amount.compareTo(order.getTotalAmount()) != 0) {
            throw new PaymentAmountMismatchException(orderId);
        }
        // 결제 승인 전 예약 상태 사전 가드(GH #3 설계 D-B) — 리퍼가 이미 해제한 예약이면 PG 승인
        // 전에 거절해 만료↔결제 경합 창을 좁힌다. CONFIRMED는 이전 시도가 재고 확정까지 마치고
        // 끊긴 재시도 경로라 통과시킨다.
        InventoryPort.ReservationState reservationState = inventoryPort.status(orderId);
        if (reservationState != InventoryPort.ReservationState.RESERVED
                && reservationState != InventoryPort.ReservationState.CONFIRMED) {
            throw new ReservationNotActiveException(orderId, reservationState.name());
        }
        Confirmation confirmation = paymentGatewayPort.confirm(paymentKey, orderId, amount);
        order.markPaid(confirmation.paymentKey());
        order = orderRepository.save(order);
        // 재고 확정은 동기 호출하지 않는다(GH #3 S4) — OrderPaid 발행이 inventory-api의 confirm
        // 코레오그래피를 구동한다. 아웃박스(event_publication) + order-batch 스윕이 유실을 방지한다.
        eventPublisher.publishOrderPaid(order.getId(), order.getCustomerId(), order.getTotalAmount());
        return order;
    }

    @Override
    public Order cancel(String orderId, String customerId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!order.getCustomerId().equals(customerId)) {
            throw new OrderNotFoundException(orderId);
        }
        return doCancel(order, reason);
    }

    @Override
    public Order cancelByAdmin(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return doCancel(order, reason);
    }

    private Order doCancel(Order order, String reason) {
        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderCancelNotAllowedException(order.getId());
        }
        // PG 환불 선행(동기). 성공 후 로컬 실패 시 주문이 PAID로 남아 같은 엔드포인트 재시도로 수렴한다
        // (어댑터가 ALREADY_CANCELED_PAYMENT를 성공으로 매핑). 재고 재입고는 동기 호출하지 않는다
        // (GH #3 S4) — OrderCanceled 발행이 inventory-api의 restock 코레오그래피를 구동하고,
        // 아웃박스 + order-batch 스윕이 유실을 방지한다(inventory restock은 RESTOCKED 멱등).
        paymentGatewayPort.cancel(order.getPaymentKey(), reason);
        try {
            order.markCanceled();
            Order saved = orderRepository.save(order);
            eventPublisher.publishOrderCanceled(saved.getId(), saved.getCustomerId(), saved.getTotalAmount());
            return saved;
        } catch (RuntimeException e) {
            log.warn("Refund succeeded but local cancel failed for orderId=" + order.getId()
                    + ", paymentKey=" + order.getPaymentKey() + " — order stays PAID, retry cancel", e);
            throw e;
        }
    }

    @Override
    public void expire(String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.markExpired();
            orderRepository.save(order);
        });
    }
}
