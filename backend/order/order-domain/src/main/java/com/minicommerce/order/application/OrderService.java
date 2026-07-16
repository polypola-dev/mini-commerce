package com.minicommerce.order.application;

import com.minicommerce.order.application.port.in.ConfirmPaymentUseCase;
import com.minicommerce.order.application.port.in.ExpireOrderUseCase;
import com.minicommerce.order.application.port.in.GetOrdersUseCase;
import com.minicommerce.order.application.port.in.PlaceOrderUseCase;
import com.minicommerce.order.application.port.out.InventoryPort;
import com.minicommerce.order.application.port.out.InventoryPort.StockHold;
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
import com.minicommerce.order.domain.exception.OrderNotFoundException;
import com.minicommerce.order.domain.exception.PaymentAmountMismatchException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService implements PlaceOrderUseCase, ConfirmPaymentUseCase, GetOrdersUseCase, ExpireOrderUseCase {

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
        List<StockItem> stockItems = command.items().stream()
                .map(i -> new StockItem(i.productId(), i.quantity()))
                .toList();
        StockHold hold = inventoryPort.reserve(stockItems);

        try {
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

            Order order = orderRepository.save(new Order(
                    UUID.randomUUID().toString(), customerId, lines,
                    command.shippingRecipient(), command.shippingPhone(),
                    command.shippingAddress(), command.shippingDetailAddress(), command.shippingZipCode()
            ));
            inventoryPort.createReservationForOrder(order.getId(), hold);
            eventPublisher.publishOrderPlaced(order.getId(), order.getCustomerId(), order.getTotalAmount());
            return order;
        } catch (RuntimeException e) {
            inventoryPort.release(hold);
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
        Confirmation confirmation = paymentGatewayPort.confirm(paymentKey, orderId, amount);
        order.markPaid(confirmation.paymentKey());
        order = orderRepository.save(order);
        inventoryPort.confirmByOrderId(orderId);
        eventPublisher.publishOrderPaid(order.getId(), order.getCustomerId(), order.getTotalAmount());
        return order;
    }

    @Override
    public void expire(String orderId) {
        orderRepository.findById(orderId).ifPresent(order -> {
            order.markExpired();
            orderRepository.save(order);
        });
    }
}
