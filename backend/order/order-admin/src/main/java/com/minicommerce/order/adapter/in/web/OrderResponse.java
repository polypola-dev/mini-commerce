package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String orderId,
        String orderNumber,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderLineResponse> lines,
        String shippingRecipient,
        String shippingPhone,
        String shippingAddress,
        String shippingDetailAddress,
        String shippingZipCode
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getLines().stream().map(OrderLineResponse::from).toList(),
                order.getShippingRecipient(),
                order.getShippingPhone(),
                order.getShippingAddress(),
                order.getShippingDetailAddress(),
                order.getShippingZipCode()
        );
    }
}
