package com.minicommerce.order.application.port.out;

import com.minicommerce.order.domain.Order;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(String id);
    List<Order> findAllByCustomerId(String customerId);
    List<Order> findAll();
}
