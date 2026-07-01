package com.minicommerce.review;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> reviews,
        double averageRating,
        int totalCount
) {
    static ReviewListResponse from(List<Review> reviews) {
        double avg = reviews.isEmpty() ? 0.0
                : reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return new ReviewListResponse(
                reviews.stream().map(ReviewResponse::from).toList(),
                Math.round(avg * 10.0) / 10.0,
                reviews.size()
        );
    }
}
