package com.minicommerce.global;

import com.minicommerce.global.security.AdminAuthorizationFilter;
import com.minicommerce.global.security.InternalAuthFilter;
import com.minicommerce.global.security.JwtVerificationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * shop-api 부팅 앱의 웹 설정 — 자기 소관 경로(reviews/cart/notifications, admin)만 배선한다.
 * JwtVerificationFilter/AdminAuthorizationFilter는 shared-web/global/security에서 재사용한다.
 * order 웹계층은 order-api로 이관됐으므로 여기서 배선하지 않는다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final List<String> allowedOrigins;
    private final String jwksUrl;
    private final String bffSecretKey;
    private final String internalApiKey;

    public WebConfig(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins,
            @Value("${app.security.jwks-url}") String jwksUrl,
            @Value("${app.security.bff-secret-key}") String bffSecretKey,
            @Value("${app.security.internal-api-key}") String internalApiKey
    ) {
        this.allowedOrigins = allowedOrigins;
        this.jwksUrl = jwksUrl;
        this.bffSecretKey = bffSecretKey;
        this.internalApiKey = internalApiKey;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Bean
    public FilterRegistrationBean<JwtVerificationFilter> jwtVerificationFilter() {
        FilterRegistrationBean<JwtVerificationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new JwtVerificationFilter(jwksUrl, bffSecretKey));
        registrationBean.addUrlPatterns(
                "/api/reviews",
                "/api/reviews/*",
                "/api/cart",
                "/api/cart/*",
                "/api/notifications",
                "/api/addresses",
                "/api/addresses/*",
                "/api/wishlist",
                "/api/wishlist/*"
        );
        registrationBean.setOrder(1);
        return registrationBean;
    }

    /**
     * catalog의 {@code /internal/products/**} 서비스간 인증(B3, ADR-020). 호출자는 order-api다.
     * {@code /actuator/**}는 의도적으로 대상 밖 — F2의 probe 경로는 무인증이어야 kubelet이 접근한다.
     */
    @Bean
    public FilterRegistrationBean<InternalAuthFilter> internalAuthFilter() {
        FilterRegistrationBean<InternalAuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new InternalAuthFilter(internalApiKey));
        registrationBean.addUrlPatterns("/internal/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<AdminAuthorizationFilter> adminAuthorizationFilter() {
        FilterRegistrationBean<AdminAuthorizationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new AdminAuthorizationFilter(jwksUrl, bffSecretKey));
        registrationBean.addUrlPatterns("/api/admin/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
