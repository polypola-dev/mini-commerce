package com.minicommerce.review;

import com.minicommerce.catalog.ProductRepository;
import com.minicommerce.shared.UuidV7;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    List<Review> getReviewsForProduct(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(UUID.fromString(productId));
    }

    Review createReview(String productId, String authorId, int rating, String content) {
        UUID productUuid = UUID.fromString(productId);
        if (!productRepository.existsById(productUuid)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }
        return reviewRepository.save(new Review(UuidV7.randomUUID(), productUuid, UUID.fromString(authorId), rating, content));
    }

    void deleteReview(String reviewId, String authorId) {
        Review review = reviewRepository.findById(UUID.fromString(reviewId))
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));
        if (!review.getAuthorId().equals(UUID.fromString(authorId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another user's review");
        }
        reviewRepository.delete(review);
    }
}
