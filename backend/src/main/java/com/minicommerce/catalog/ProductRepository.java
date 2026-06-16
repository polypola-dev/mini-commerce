package com.minicommerce.catalog;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByActiveTrueOrderByNameAsc();

    @Query("SELECT p FROM Product p WHERE p.active = true AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY p.name ASC")
    List<Product> searchActive(@Param("q") String q);

    @Query(value = "SELECT p FROM Product p WHERE " +
                   "(:active IS NULL OR p.active = :active) AND " +
                   "(:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE " +
                        "(:active IS NULL OR p.active = :active) AND " +
                        "(:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Product> findWithFilters(@Param("active") Boolean active, @Param("q") String q, Pageable pageable);
}
