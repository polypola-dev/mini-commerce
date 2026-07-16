package com.minicommerce.order.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentGatewayPort {
    Confirmation confirm(String paymentKey, String orderId, BigDecimal amount);

    record Confirmation(String paymentKey, String method, Instant approvedAt) {}
}
