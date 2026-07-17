package com.minicommerce.order.application.port.out;

import java.math.BigDecimal;

public interface OrderEventPublisher {
    void publishOrderPlaced(String orderId, String customerId, BigDecimal amount);
    void publishOrderPaid(String orderId, String customerId, BigDecimal amount);
    void publishOrderCanceled(String orderId, String customerId, BigDecimal amount);
}
