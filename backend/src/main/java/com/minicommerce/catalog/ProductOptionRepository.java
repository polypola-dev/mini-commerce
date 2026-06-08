package com.minicommerce.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionRepository extends JpaRepository<ProductOption, String> {
    List<ProductOption> findByProductId(String productId);
}
