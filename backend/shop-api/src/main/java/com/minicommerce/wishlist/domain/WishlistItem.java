package com.minicommerce.wishlist.domain;

import java.time.Instant;

/**
 * 위시리스트 항목 — 순수 도메인 POJO (기술 애너테이션 0).
 *
 * <p>소유자({@code customerId}, Supabase user id)가 찜한 상품({@code productId}) 하나를 나타낸다.
 * (customerId, productId) 조합은 유일하다.
 */
public class WishlistItem {

    private final String id;
    private final String customerId;
    private final String productId;
    private final Instant createdAt;

    public WishlistItem(String id, String customerId, String productId, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getProductId() {
        return productId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
