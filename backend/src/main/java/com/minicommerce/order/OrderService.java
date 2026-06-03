package com.minicommerce.order;

import com.minicommerce.catalog.Product;
import com.minicommerce.catalog.ProductRepository;
import com.minicommerce.inventory.InventoryHold;
import com.minicommerce.inventory.InventoryItem;
import com.minicommerce.inventory.InventoryReservation;
import com.minicommerce.inventory.InventoryReservationRepository;
import com.minicommerce.inventory.InventoryService;
import com.minicommerce.inventory.ReservationLine;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryService inventoryService;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            InventoryReservationRepository reservationRepository,
            InventoryService inventoryService,
            TransactionTemplate transactionTemplate,
            ApplicationEventPublisher eventPublisher
    ) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
        this.transactionTemplate = transactionTemplate;
        this.eventPublisher = eventPublisher;
    }

    public Order createOrder(CreateOrderRequest request, String customerId) {
        List<InventoryItem> inventoryItems = request.items().stream()
                .map(item -> new InventoryItem(item.productId(), item.quantity()))
                .toList();
        InventoryHold hold = inventoryService.reserve(inventoryItems);

        try {
            return transactionTemplate.execute(status -> createOrderTransaction(request, customerId, hold));
        } catch (RuntimeException exception) {
            inventoryService.release(hold);
            throw exception;
        }
    }

    private Order createOrderTransaction(CreateOrderRequest request, String customerId, InventoryHold hold) {
        List<OrderLineDraft> lines = request.items().stream()
                .map(item -> {
                    Product product = productRepository.findById(item.productId())
                            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + item.productId()));
                    return new OrderLineDraft(product.getId(), product.getName(), product.getPrice(), item.quantity());
                })
                .toList();

        Order order = orderRepository.save(new Order(UUID.randomUUID().toString(), customerId, lines));
        reservationRepository.save(new InventoryReservation(
                hold.reservationId(),
                order.getId(),
                hold.expiresAt(),
                hold.items().stream()
                        .map(item -> new ReservationLine(item.productId(), item.quantity()))
                        .toList()
        ));
        eventPublisher.publishEvent(new OrderPlacedEvent(order.getId(), order.getCustomerId(), order.getTotalAmount()));
        return order;
    }

    public Order completeFakePayment(String orderId) {
        return transactionTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
            InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Reservation not found for order: " + orderId));

            order.markPaid();
            reservation.confirm();
            inventoryService.confirm(reservation.getId());
            eventPublisher.publishEvent(new OrderPaidEvent(order.getId(), order.getCustomerId(), order.getTotalAmount()));
            return order;
        });
    }
}
