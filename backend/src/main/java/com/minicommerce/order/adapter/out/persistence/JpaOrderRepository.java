package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.domain.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaOrderRepository extends JpaRepository<Order, String> {
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.lines WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<Order> findAllByCustomerIdWithLines(String customerId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.lines ORDER BY o.createdAt DESC")
    List<Order> findAllWithLines();
}
