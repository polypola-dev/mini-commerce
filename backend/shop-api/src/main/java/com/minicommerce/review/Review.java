package com.minicommerce.review;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    private UUID id;

    private UUID productId;
    private UUID authorId;
    private int rating;
    private String content;
    private Instant createdAt;

    protected Review() {
    }

    public Review(UUID id, UUID productId, UUID authorId, int rating, String content) {
        this.id = id;
        this.productId = productId;
        this.authorId = authorId;
        this.rating = rating;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public int getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
