package com.minicommerce.order.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    PAYMENT_FAILED,
    EXPIRED,
    CANCELED,
    SHIPPED,
    DELIVERED
}
