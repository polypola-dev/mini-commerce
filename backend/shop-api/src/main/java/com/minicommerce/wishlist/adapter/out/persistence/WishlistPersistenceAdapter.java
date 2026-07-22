package com.minicommerce.wishlist.adapter.out.persistence;

import com.minicommerce.wishlist.application.port.out.WishlistRepositoryPort;
import com.minicommerce.wishlist.domain.WishlistItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class WishlistPersistenceAdapter implements WishlistRepositoryPort {

    private final WishlistJpaRepository jpaRepository;

    WishlistPersistenceAdapter(WishlistJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<WishlistItem> findByCustomerId(String customerId) {
        return jpaRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(e -> new WishlistItem(e.getId(), e.getCustomerId(), e.getProductId(), e.getCreatedAt()))
                .toList();
    }

    @Override
    public boolean exists(String customerId, String productId) {
        return jpaRepository.existsByCustomerIdAndProductId(customerId, productId);
    }

    @Override
    public WishlistItem save(WishlistItem item) {
        WishlistJpaEntity saved = jpaRepository.save(new WishlistJpaEntity(
                item.getId(), item.getCustomerId(), item.getProductId(), item.getCreatedAt()));
        return new WishlistItem(saved.getId(), saved.getCustomerId(), saved.getProductId(), saved.getCreatedAt());
    }

    @Override
    public void delete(String customerId, String productId) {
        jpaRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }
}
