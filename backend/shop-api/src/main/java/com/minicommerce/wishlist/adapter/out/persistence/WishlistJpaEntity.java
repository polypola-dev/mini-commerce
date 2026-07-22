package com.minicommerce.wishlist.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wishlist_items")
public class WishlistJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "created_at")
    private Instant createdAt;

    protected WishlistJpaEntity() {
    }

    public WishlistJpaEntity(UUID id, UUID customerId, UUID productId, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
