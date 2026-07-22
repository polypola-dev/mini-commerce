package com.minicommerce.wishlist.adapter.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface WishlistJpaRepository extends JpaRepository<WishlistJpaEntity, UUID> {

    List<WishlistJpaEntity> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    boolean existsByCustomerIdAndProductId(UUID customerId, UUID productId);

    @Transactional
    void deleteByCustomerIdAndProductId(UUID customerId, UUID productId);
}
