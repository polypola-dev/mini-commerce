package com.minicommerce.order.application.port.in;

import com.minicommerce.order.application.PlaceOrderCommand;
import com.minicommerce.order.domain.Order;

public interface PlaceOrderUseCase {
    Order place(PlaceOrderCommand command, String customerId);
}
