package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.domain.Order;
import com.minicommerce.order.domain.OrderStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaOrderRepository extends JpaRepository<Order, String> {
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.lines WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<Order> findAllByCustomerIdWithLines(String customerId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.lines ORDER BY o.createdAt DESC")
    List<Order> findAllWithLines();

    @Query(value = "SELECT o.id FROM Order o WHERE " +
                   "(:status IS NULL OR o.status = :status) AND " +
                   "(:q IS NULL OR LOWER(CAST(o.id AS String)) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%') OR " +
                   "LOWER(COALESCE(o.customerId, '')) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%'))",
           countQuery = "SELECT COUNT(o) FROM Order o WHERE " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:q IS NULL OR LOWER(CAST(o.id AS String)) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%') OR " +
                        "LOWER(COALESCE(o.customerId, '')) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%'))")
    Page<String> findOrderIdsPaged(@Param("status") OrderStatus status,
                                   @Param("q") String q,
                                   Pageable pageable);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.lines WHERE o.id IN :ids")
    List<Order> findByIdsWithLines(@Param("ids") List<String> ids);
}
