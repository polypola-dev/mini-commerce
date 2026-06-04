package com.minicommerce.review;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class ReviewController {
    private final ReviewService reviewService;

    ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/products/{productId}/reviews")
    ReviewListResponse getReviews(@PathVariable String productId) {
        return ReviewListResponse.from(reviewService.getReviewsForProduct(productId));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    ReviewResponse createReview(@RequestBody @Valid CreateReviewRequest request, HttpServletRequest httpRequest) {
        String authorId = (String) httpRequest.getAttribute("authenticatedUserId");
        return ReviewResponse.from(reviewService.createReview(request.productId(), authorId, request.rating(), request.content()));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteReview(@PathVariable String reviewId, HttpServletRequest httpRequest) {
        String authorId = (String) httpRequest.getAttribute("authenticatedUserId");
        reviewService.deleteReview(reviewId, authorId);
    }
}
