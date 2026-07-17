package com.minicommerce.order.adapter.out.event;

import com.minicommerce.order.application.port.out.OrderEventPublisher;
import com.minicommerce.order.OrderCanceledEvent;
import com.minicommerce.order.OrderPaidEvent;
import com.minicommerce.order.OrderPlacedEvent;
import java.math.BigDecimal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringOrderEventAdapter implements OrderEventPublisher {

    private final ApplicationEventPublisher springPublisher;

    public SpringOrderEventAdapter(ApplicationEventPublisher springPublisher) {
        this.springPublisher = springPublisher;
    }

    @Override
    public void publishOrderPlaced(String orderId, String customerId, BigDecimal amount) {
        springPublisher.publishEvent(new OrderPlacedEvent(orderId, customerId, amount));
    }

    @Override
    public void publishOrderPaid(String orderId, String customerId, BigDecimal amount) {
        springPublisher.publishEvent(new OrderPaidEvent(orderId, customerId, amount));
    }

    @Override
    public void publishOrderCanceled(String orderId, String customerId, BigDecimal amount) {
        springPublisher.publishEvent(new OrderCanceledEvent(orderId, customerId, amount));
    }
}
