package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.application.port.out.OrderRepository;
import com.minicommerce.order.domain.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceAdapter implements OrderRepository {

    private final JpaOrderRepository jpaRepository;

    public OrderPersistenceAdapter(JpaOrderRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Order> findAllByCustomerId(String customerId) {
        return jpaRepository.findAllByCustomerIdWithLines(customerId);
    }

    @Override
    public List<Order> findAll() {
        return jpaRepository.findAllWithLines();
    }
}
