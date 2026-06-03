package com.minicommerce.order;

import java.math.BigDecimal;

public record OrderPaidEvent(String orderId, String customerId, BigDecimal totalAmount) {
}
