package com.minicommerce.wishlist.application.port.out;

import com.minicommerce.wishlist.domain.WishlistItem;
import java.util.List;

/** 위시리스트 영속성 포트 (Driven Port). */
public interface WishlistRepositoryPort {

    /** 소유자의 위시리스트 항목을 찜한 최신순으로 조회. */
    List<WishlistItem> findByCustomerId(String customerId);

    boolean exists(String customerId, String productId);

    WishlistItem save(WishlistItem item);

    void delete(String customerId, String productId);
}
