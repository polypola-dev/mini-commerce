package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderStatus;
import java.math.BigDecimal;

public record OrderResponse(String orderId, OrderStatus status, BigDecimal totalAmount) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount());
    }
}
