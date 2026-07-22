package com.minicommerce.wishlist.adapter.out.persistence;

import com.minicommerce.wishlist.application.port.out.WishlistRepositoryPort;
import com.minicommerce.wishlist.domain.WishlistItem;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class WishlistPersistenceAdapter implements WishlistRepositoryPort {

    private final WishlistJpaRepository jpaRepository;

    WishlistPersistenceAdapter(WishlistJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<WishlistItem> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerIdOrderByCreatedAtDesc(UUID.fromString(customerId)).stream()
                .map(e -> new WishlistItem(e.getId().toString(), e.getCustomerId().toString(),
                        e.getProductId().toString(), e.getCreatedAt()))
                .toList();
    }

    @Override
    public boolean exists(String customerId, String productId) {
        return jpaRepository.existsByCustomerIdAndProductId(UUID.fromString(customerId), UUID.fromString(productId));
    }

    @Override
    public WishlistItem save(WishlistItem item) {
        WishlistJpaEntity saved = jpaRepository.save(new WishlistJpaEntity(
                UUID.fromString(item.getId()), UUID.fromString(item.getCustomerId()),
                UUID.fromString(item.getProductId()), item.getCreatedAt()));
        return new WishlistItem(saved.getId().toString(), saved.getCustomerId().toString(),
                saved.getProductId().toString(), saved.getCreatedAt());
    }

    @Override
    public void delete(String customerId, String productId) {
        jpaRepository.deleteByCustomerIdAndProductId(UUID.fromString(customerId), UUID.fromString(productId));
    }
}
