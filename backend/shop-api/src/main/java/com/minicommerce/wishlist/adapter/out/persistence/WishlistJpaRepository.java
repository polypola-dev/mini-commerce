package com.minicommerce.wishlist.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface WishlistJpaRepository extends JpaRepository<WishlistJpaEntity, String> {

    List<WishlistJpaEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    boolean existsByCustomerIdAndProductId(String customerId, String productId);

    @Transactional
    void deleteByCustomerIdAndProductId(String customerId, String productId);
}
