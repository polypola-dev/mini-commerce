package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.domain.OrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JpaOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.lines WHERE o.id = :id")
    Optional<OrderJpaEntity> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.lines WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<OrderJpaEntity> findAllByCustomerIdWithLines(UUID customerId);

    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.lines ORDER BY o.createdAt DESC")
    List<OrderJpaEntity> findAllWithLines();

    // 관리자 주문 검색(orderId/customerId 부분 문자열 매칭). id/customerId가 uuid로 바뀌었으므로
    // CAST(x AS String)으로 텍스트 표현(정규화된 소문자 uuid)으로 변환해 LIKE 비교한다.
    // COALESCE도 문자열 캐스팅 이후에 적용(uuid에 직접 '' 기본값을 줄 수 없음).
    @Query(value = "SELECT o.id FROM OrderJpaEntity o WHERE " +
                   "(:status IS NULL OR o.status = :status) AND " +
                   "(:q IS NULL OR LOWER(CAST(o.id AS String)) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%') OR " +
                   "LOWER(COALESCE(CAST(o.customerId AS String), '')) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%'))",
           countQuery = "SELECT COUNT(o) FROM OrderJpaEntity o WHERE " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:q IS NULL OR LOWER(CAST(o.id AS String)) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%') OR " +
                        "LOWER(COALESCE(CAST(o.customerId AS String), '')) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%'))")
    Page<UUID> findOrderIdsPaged(@Param("status") OrderStatus status,
                                 @Param("q") String q,
                                 Pageable pageable);

    @Query("SELECT o FROM OrderJpaEntity o LEFT JOIN FETCH o.lines WHERE o.id IN :ids")
    List<OrderJpaEntity> findByIdsWithLines(@Param("ids") List<UUID> ids);
}
