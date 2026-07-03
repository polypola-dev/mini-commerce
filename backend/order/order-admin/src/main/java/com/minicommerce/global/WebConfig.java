package com.minicommerce.global;

import com.minicommerce.global.security.AdminAuthorizationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * order-admin 부팅 앱의 웹 설정 — 관리자 주문 경로만 배선한다(ADR-005 S4).
 * AdminAuthorizationFilter는 shared-web/global/security에서 재사용한다.
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
    public FilterRegistrationBean<AdminAuthorizationFilter> adminAuthorizationFilter() {
        FilterRegistrationBean<AdminAuthorizationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new AdminAuthorizationFilter(jwksUrl, bffSecretKey));
        registrationBean.addUrlPatterns("/api/admin/orders/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
