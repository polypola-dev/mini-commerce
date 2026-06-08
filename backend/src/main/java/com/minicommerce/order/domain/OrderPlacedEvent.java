package com.minicommerce.order.domain;

import java.math.BigDecimal;

public record OrderPlacedEvent(String orderId, String customerId, BigDecimal totalAmount) {
}
