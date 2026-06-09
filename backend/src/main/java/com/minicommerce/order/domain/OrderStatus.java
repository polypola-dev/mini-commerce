package com.minicommerce.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    CANCELED,
    SHIPPED,
    DELIVERED
}
