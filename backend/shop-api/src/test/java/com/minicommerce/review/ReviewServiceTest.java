package com.minicommerce.review;

import com.minicommerce.catalog.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, productRepository);
    }

    @Test
    @DisplayName("성공: 상품 리뷰 목록 조회 시 findByProductIdOrderByCreatedAtDesc를 호출한다")
    void getReviewsForProduct_ShouldCallRepositoryMethod() {
        // given
        String productId = "prod-1";
        List<Review> expectedReviews = List.of(
                new Review("review-1", productId, "user-1", 5, "정말 좋아요")
        );
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)).thenReturn(expectedReviews);

        // when
        List<Review> result = reviewService.getReviewsForProduct(productId);

        // then
        assertThat(result).isEqualTo(expectedReviews);
        verify(reviewRepository).findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Test
    @DisplayName("성공: 상품이 존재하면 리뷰를 저장하고 저장된 리뷰를 반환한다")
    void createReview_Success() {
        // given
        String productId = "prod-1";
        String authorId = "user-1";
        int rating = 4;
        String content = "좋은 상품입니다";
        Review savedReview = new Review("review-1", productId, authorId, rating, content);

        when(productRepository.existsById(productId)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // when
        Review result = reviewService.createReview(productId, authorId, rating, content);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("review-1");
        assertThat(result.getRating()).isEqualTo(rating);
        verify(productRepository).existsById(productId);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("실패: 상품이 존재하지 않으면 EntityNotFoundException을 던진다")
    void createReview_ProductNotFound_ThrowsEntityNotFoundException() {
        // given
        String productId = "prod-not-found";
        when(productRepository.existsById(productId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(productId, "user-1", 4, "내용"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");

        // 상품이 없으므로 리뷰 저장이 호출되지 않아야 함
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공: 작성자 ID가 일치하면 리뷰를 삭제한다")
    void deleteReview_Success() {
        // given
        String reviewId = "review-1";
        String authorId = "user-1";
        Review review = new Review(reviewId, "prod-1", authorId, 5, "좋아요");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // when
        reviewService.deleteReview(reviewId, authorId);

        // then
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("실패: 다른 사용자의 리뷰 삭제 시도 시 403 FORBIDDEN ResponseStatusException을 던진다")
    void deleteReview_WrongAuthor_ThrowsForbidden() {
        // given
        String reviewId = "review-1";
        Review review = new Review(reviewId, "prod-1", "original-author", 5, "좋아요");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, "another-user"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        // 권한이 없으므로 삭제가 호출되지 않아야 함
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 리뷰 삭제 시 EntityNotFoundException을 던진다")
    void deleteReview_NotFound_ThrowsEntityNotFoundException() {
        // given
        String reviewId = "review-not-found";
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, "user-1"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Review not found");
    }
}
