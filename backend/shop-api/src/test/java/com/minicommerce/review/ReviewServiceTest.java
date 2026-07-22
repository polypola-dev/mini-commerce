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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    // id들이 uuid로 전환됐으므로(GH #20) 유효 UUID 문자열을 쓴다.
    private static final String PROD_1 = "00000000-0000-7000-8000-0000000000a1";
    private static final UUID PROD_1_UUID = UUID.fromString(PROD_1);
    private static final String USER_1 = "00000000-0000-7000-8000-0000000000c1";
    private static final UUID USER_1_UUID = UUID.fromString(USER_1);
    private static final String REVIEW_1 = "00000000-0000-7000-8000-0000000000d1";
    private static final UUID REVIEW_1_UUID = UUID.fromString(REVIEW_1);

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
        List<Review> expectedReviews = List.of(
                new Review(REVIEW_1_UUID, PROD_1_UUID, USER_1_UUID, 5, "정말 좋아요")
        );
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(PROD_1_UUID)).thenReturn(expectedReviews);

        // when
        List<Review> result = reviewService.getReviewsForProduct(PROD_1);

        // then
        assertThat(result).isEqualTo(expectedReviews);
        verify(reviewRepository).findByProductIdOrderByCreatedAtDesc(PROD_1_UUID);
    }

    @Test
    @DisplayName("성공: 상품이 존재하면 리뷰를 저장하고 저장된 리뷰를 반환한다")
    void createReview_Success() {
        // given
        int rating = 4;
        String content = "좋은 상품입니다";
        Review savedReview = new Review(REVIEW_1_UUID, PROD_1_UUID, USER_1_UUID, rating, content);

        when(productRepository.existsById(PROD_1_UUID)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        // when
        Review result = reviewService.createReview(PROD_1, USER_1, rating, content);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(REVIEW_1_UUID);
        assertThat(result.getRating()).isEqualTo(rating);
        verify(productRepository).existsById(PROD_1_UUID);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("실패: 상품이 존재하지 않으면 EntityNotFoundException을 던진다")
    void createReview_ProductNotFound_ThrowsEntityNotFoundException() {
        // given
        String productId = "00000000-0000-7000-8000-00000000ffff";
        when(productRepository.existsById(UUID.fromString(productId))).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(productId, USER_1, 4, "내용"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Product not found");

        // 상품이 없으므로 리뷰 저장이 호출되지 않아야 함
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("성공: 작성자 ID가 일치하면 리뷰를 삭제한다")
    void deleteReview_Success() {
        // given
        Review review = new Review(REVIEW_1_UUID, PROD_1_UUID, USER_1_UUID, 5, "좋아요");

        when(reviewRepository.findById(REVIEW_1_UUID)).thenReturn(Optional.of(review));

        // when
        reviewService.deleteReview(REVIEW_1, USER_1);

        // then
        verify(reviewRepository).delete(review);
    }

    @Test
    @DisplayName("실패: 다른 사용자의 리뷰 삭제 시도 시 403 FORBIDDEN ResponseStatusException을 던진다")
    void deleteReview_WrongAuthor_ThrowsForbidden() {
        // given
        String originalAuthor = "00000000-0000-7000-8000-0000000000c2";
        String anotherUser = "00000000-0000-7000-8000-0000000000c3";
        Review review = new Review(REVIEW_1_UUID, PROD_1_UUID, UUID.fromString(originalAuthor), 5, "좋아요");

        when(reviewRepository.findById(REVIEW_1_UUID)).thenReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_1, anotherUser))
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
        String reviewId = "00000000-0000-7000-8000-00000000eeee";
        when(reviewRepository.findById(UUID.fromString(reviewId))).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, USER_1))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Review not found");
    }
}
