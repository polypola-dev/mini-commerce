package com.minicommerce.order;

import java.math.BigDecimal;

public record OrderPlacedEvent(String orderId, String customerId, BigDecimal totalAmount) {
}
