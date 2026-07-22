package com.minicommerce.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByActiveTrueOrderByNameAsc();

    // GH #22 — SKU 중복 방어. 생성은 존재 여부만, 수정은 자기 자신 제외.
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    @Query("SELECT p FROM Product p WHERE p.active = true AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY p.name ASC")
    List<Product> searchActive(@Param("q") String q);

    @Query(value = "SELECT p FROM Product p WHERE " +
                   "(:active IS NULL OR p.active = :active) AND " +
                   "(:q IS NULL OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%') OR LOWER(p.description) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%'))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "(:active IS NULL OR p.active = :active) AND " +
                        "(:q IS NULL OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%') OR LOWER(p.description) LIKE CONCAT('%', LOWER(CAST(:q AS String)), '%'))")
    Page<Product> findWithFilters(@Param("active") Boolean active, @Param("q") String q, Pageable pageable);
}
