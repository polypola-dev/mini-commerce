package com.minicommerce.order.application.port.in;

import com.minicommerce.order.domain.Order;
import java.math.BigDecimal;

public interface ConfirmPaymentUseCase {
    Order confirm(String orderId, String customerId, String paymentKey, BigDecimal amount);
}
