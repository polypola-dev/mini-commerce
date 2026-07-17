package com.minicommerce.order.application.port.in;

import com.minicommerce.order.domain.Order;

public interface CancelOrderUseCase {
    Order cancel(String orderId, String customerId, String reason);

    Order cancelByAdmin(String orderId, String reason);
}
