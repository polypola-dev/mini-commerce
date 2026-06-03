package com.minicommerce.order;

import java.math.BigDecimal;

public record OrderResponse(String orderId, OrderStatus status, BigDecimal totalAmount) {
    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount());
    }
}
