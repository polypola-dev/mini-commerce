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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    // id들이 uuid로 전환됐으므로(GH #20) 응답 직렬화 시 UUID.toString()이 나온다.
    private static final String PROD_1 = "00000000-0000-7000-8000-0000000000a1";
    private static final UUID PROD_1_UUID = UUID.fromString(PROD_1);
    private static final String USER_1 = "00000000-0000-7000-8000-0000000000c1";
    private static final UUID USER_1_UUID = UUID.fromString(USER_1);
    private static final String REVIEW_1 = "00000000-0000-7000-8000-0000000000d1";
    private static final UUID REVIEW_1_UUID = UUID.fromString(REVIEW_1);

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
        Review review = new Review(REVIEW_1_UUID, PROD_1_UUID, USER_1_UUID, 5, "정말 좋아요");
        when(reviewService.getReviewsForProduct(PROD_1)).thenReturn(List.of(review));

        // when & then
        mockMvc.perform(get("/api/products/{productId}/reviews", PROD_1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.reviews[0].id").value(REVIEW_1))
                .andExpect(jsonPath("$.reviews[0].rating").value(5));
    }

    @Test
    @DisplayName("성공: 리뷰 생성 시 201 Created와 ReviewResponse JSON을 반환한다")
    void createReview_ShouldReturn201WithReviewResponse() throws Exception {
        // given
        Review savedReview = new Review(REVIEW_1_UUID, PROD_1_UUID, USER_1_UUID, 4, "좋은 상품입니다");

        when(reviewService.createReview(eq(PROD_1), eq(USER_1), eq(4), eq("좋은 상품입니다")))
                .thenReturn(savedReview);

        CreateReviewRequest request = new CreateReviewRequest(PROD_1, 4, "좋은 상품입니다");

        // when & then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        // authenticatedUserId 속성을 요청에 직접 주입 (JWT 필터 우회)
                        .requestAttr("authenticatedUserId", USER_1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(REVIEW_1))
                .andExpect(jsonPath("$.productId").value(PROD_1))
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    @DisplayName("성공: 리뷰 삭제 시 204 No Content를 반환한다")
    void deleteReview_ShouldReturn204() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/reviews/{reviewId}", REVIEW_1)
                        .requestAttr("authenticatedUserId", USER_1))
                .andExpect(status().isNoContent());

        verify(reviewService).deleteReview(REVIEW_1, USER_1);
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
                        .requestAttr("authenticatedUserId", USER_1))
                .andExpect(status().isBadRequest());
    }
}
