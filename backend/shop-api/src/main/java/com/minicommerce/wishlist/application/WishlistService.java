package com.minicommerce.wishlist.application;

import com.minicommerce.wishlist.application.port.in.ManageWishlistUseCase;
import com.minicommerce.wishlist.application.port.out.WishlistRepositoryPort;
import com.minicommerce.wishlist.domain.WishlistItem;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishlistService implements ManageWishlistUseCase {

    private final WishlistRepositoryPort wishlistRepository;

    public WishlistService(WishlistRepositoryPort wishlistRepository) {
        this.wishlistRepository = wishlistRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listProductIds(String customerId) {
        return wishlistRepository.findByCustomerId(customerId).stream()
                .map(WishlistItem::getProductId)
                .toList();
    }

    @Override
    public void add(String customerId, String productId) {
        if (wishlistRepository.exists(customerId, productId)) {
            return;
        }
        wishlistRepository.save(new WishlistItem(
                UUID.randomUUID().toString(), customerId, productId, Instant.now()));
    }

    @Override
    public void remove(String customerId, String productId) {
        wishlistRepository.delete(customerId, productId);
    }
}
