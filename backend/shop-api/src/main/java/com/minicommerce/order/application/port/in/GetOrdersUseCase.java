package com.minicommerce.order.application.port.in;

import com.minicommerce.order.domain.Order;
import java.util.List;

public interface GetOrdersUseCase {
    List<Order> getOrders(String customerId);
    Order getOrder(String orderId, String customerId);
}
