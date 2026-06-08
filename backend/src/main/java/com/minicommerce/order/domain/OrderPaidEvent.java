package com.minicommerce.order.domain;

import java.math.BigDecimal;

public record OrderPaidEvent(String orderId, String customerId, BigDecimal totalAmount) {
}
