package com.minicommerce.global;

import com.minicommerce.global.security.JwtVerificationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
        registrationBean.addUrlPatterns("/api/orders/*", "/api/reviews", "/api/reviews/*", "/api/cart", "/api/cart/*");
        return registrationBean;
    }
}
