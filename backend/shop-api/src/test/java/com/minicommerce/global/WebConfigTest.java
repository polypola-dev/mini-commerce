package com.minicommerce.global;

import static org.assertj.core.api.Assertions.assertThat;

import com.minicommerce.global.security.JwtVerificationFilter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * JWT 검증 필터가 사용자별 데이터 경로 전체를 덮는지 검증한다. 새 사용자 스코프 컨트롤러
 * (예: C3 addresses, C4 wishlist)를 추가할 때 이 URL 패턴 배선을 빠뜨리면 컨트롤러는
 * authenticatedUserId=null을 받아 customer_id NOT NULL 위반(500)을 낸다 — 그 회귀를 막는다.
 */
class WebConfigTest {

    private WebConfig webConfig() {
        return new WebConfig(List.of("http://localhost:3000"), "https://example/jwks.json", "bff-secret", "internal-key");
    }

    @Test
    @DisplayName("JWT 필터가 사용자별 경로(reviews/cart/notifications/addresses/wishlist)를 모두 덮는다")
    void jwtFilterCoversAllUserScopedPaths() {
        FilterRegistrationBean<JwtVerificationFilter> bean = webConfig().jwtVerificationFilter();

        assertThat(bean.getUrlPatterns()).contains(
                "/api/reviews", "/api/reviews/*",
                "/api/cart", "/api/cart/*",
                "/api/notifications",
                "/api/addresses", "/api/addresses/*",
                "/api/wishlist", "/api/wishlist/*"
        );
    }
}
