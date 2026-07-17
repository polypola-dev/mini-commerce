package com.minicommerce.order.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentGatewayPort {
    Confirmation confirm(String paymentKey, String orderId, BigDecimal amount);

    Cancellation cancel(String paymentKey, String cancelReason);

    record Confirmation(String paymentKey, String method, Instant approvedAt) {}

    record Cancellation(Instant canceledAt) {}
}
