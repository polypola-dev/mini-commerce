package com.minicommerce.review;

import java.time.Instant;

public record ReviewResponse(
        String id,
        String productId,
        String authorId,
        int rating,
        String content,
        Instant createdAt
) {
    static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getAuthorId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
