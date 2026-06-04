package com.minicommerce.review;

import com.minicommerce.catalog.ProductRepository;
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
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    Review createReview(String productId, String authorId, int rating, String content) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("Product not found: " + productId);
        }
        return reviewRepository.save(new Review(UUID.randomUUID().toString(), productId, authorId, rating, content));
    }

    void deleteReview(String reviewId, String authorId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found: " + reviewId));
        if (!review.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete another user's review");
        }
        reviewRepository.delete(review);
    }
}
