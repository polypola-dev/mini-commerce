package com.minicommerce.wishlist.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "wishlist_items")
public class WishlistJpaEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "created_at")
    private Instant createdAt;

    protected WishlistJpaEntity() {
    }

    public WishlistJpaEntity(String id, String customerId, String productId, Instant createdAt) {
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
