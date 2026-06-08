package com.minicommerce.order.application;

import com.minicommerce.order.application.port.in.CompletePaymentUseCase;
import com.minicommerce.order.application.port.in.PlaceOrderUseCase;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService implements PlaceOrderUseCase, CompletePaymentUseCase {

    private final ProductQueryPort productQueryPort;
    private final InventoryPort inventoryPort;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(
            ProductQueryPort productQueryPort,
            InventoryPort inventoryPort,
            OrderRepository orderRepository,
            OrderEventPublisher eventPublisher
    ) {
        this.productQueryPort = productQueryPort;
        this.inventoryPort = inventoryPort;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
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

            Order order = orderRepository.save(new Order(UUID.randomUUID().toString(), customerId, lines));
            inventoryPort.createReservationForOrder(order.getId(), hold);
            eventPublisher.publishOrderPlaced(order.getId(), order.getCustomerId(), order.getTotalAmount());
            return order;
        } catch (RuntimeException e) {
            inventoryPort.release(hold);
            throw e;
        }
    }

    @Override
    public Order complete(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        order.markPaid();
        inventoryPort.confirmByOrderId(orderId);
        eventPublisher.publishOrderPaid(order.getId(), order.getCustomerId(), order.getTotalAmount());
        return order;
    }
}
