package com.minicommerce.order.application.port.in;

import com.minicommerce.order.domain.Order;

public interface CompletePaymentUseCase {
    Order complete(String orderId);
}
