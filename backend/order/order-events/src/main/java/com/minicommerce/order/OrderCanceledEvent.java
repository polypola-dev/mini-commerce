package com.minicommerce.order;

import java.math.BigDecimal;

public record OrderCanceledEvent(String orderId, String orderNumber, String customerId, BigDecimal totalAmount) {
}
