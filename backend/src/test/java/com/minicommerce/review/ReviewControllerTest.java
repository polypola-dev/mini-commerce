package com.minicommerce.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReviewService reviewService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // JWT 필터를 우회하기 위해 standaloneSetup 사용 (JwtVerificationFilterTest와 동일 패턴)
        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("성공: 상품 리뷰 목록 조회 시 200 OK와 JSON을 반환한다")
    void getReviews_ShouldReturn200WithJson() throws Exception {
        // given
        String productId = "prod-1";
        Review review = new Review("review-1", productId, "user-1", 5, "정말 좋아요");
        when(reviewService.getReviewsForProduct(productId)).thenReturn(List.of(review));

        // when & then
        mockMvc.perform(get("/api/products/{productId}/reviews", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.reviews[0].id").value("review-1"))
                .andExpect(jsonPath("$.reviews[0].rating").value(5));
    }

    @Test
    @DisplayName("성공: 리뷰 생성 시 201 Created와 ReviewResponse JSON을 반환한다")
    void createReview_ShouldReturn201WithReviewResponse() throws Exception {
        // given
        String productId = "prod-1";
        String authorId = "user-1";
        Review savedReview = new Review("review-1", productId, authorId, 4, "좋은 상품입니다");

        when(reviewService.createReview(eq(productId), eq(authorId), eq(4), eq("좋은 상품입니다")))
                .thenReturn(savedReview);

        CreateReviewRequest request = new CreateReviewRequest(productId, 4, "좋은 상품입니다");

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        // authenticatedUserId 속성을 요청에 직접 주입 (JWT 필터 우회)
                        .requestAttr("authenticatedUserId", authorId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("review-1"))
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @DisplayName("성공: 리뷰 삭제 시 204 No Content를 반환한다")
    void deleteReview_ShouldReturn204() throws Exception {
        // given
        String reviewId = "review-1";
        String authorId = "user-1";

        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
                        .requestAttr("authenticatedUserId", authorId))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(reviewId, authorId);
    }

    @Test
    @DisplayName("실패: 유효하지 않은 rating(6)으로 리뷰 생성 시 400 Bad Request를 반환한다")
    void createReview_InvalidRating_ShouldReturn400() throws Exception {
        // given - rating 최대값은 5이므로 6은 @Max(5) 위반
        String requestBody = """
                {
                    "productId": "prod-1",
                    "rating": 6,
                    "content": "내용"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .requestAttr("authenticatedUserId", "user-1"))
                .andExpect(status().isBadRequest());
    }
}
