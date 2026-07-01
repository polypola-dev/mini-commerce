package com.minicommerce.review;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    private String id;

    private String productId;
    private String authorId;
    private int rating;
    private String content;
    private Instant createdAt;

    protected Review() {
    }

    public Review(String id, String productId, String authorId, int rating, String content) {
        this.id = id;
        this.productId = productId;
        this.authorId = authorId;
        this.rating = rating;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getAuthorId() {
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
