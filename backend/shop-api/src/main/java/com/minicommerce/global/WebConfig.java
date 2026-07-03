package com.minicommerce.global;

import com.minicommerce.global.security.AdminAuthorizationFilter;
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

    public WebConfig(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins,
            @Value("${app.security.jwks-url}") String jwksUrl,
            @Value("${app.security.bff-secret-key}") String bffSecretKey
    ) {
        this.allowedOrigins = allowedOrigins;
        this.jwksUrl = jwksUrl;
        this.bffSecretKey = bffSecretKey;
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
                "/api/notifications"
        );
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
