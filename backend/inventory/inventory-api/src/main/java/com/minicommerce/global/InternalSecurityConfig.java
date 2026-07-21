package com.minicommerce.global;

import com.minicommerce.global.security.InternalAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * inventory-api의 {@code /internal/**} 서비스간 인증(B3, ADR-020). 이 서비스는 웹 API가 전부
 * {@code /internal}이라(외부 ingress 미노출) shop-api/order-api와 달리 JWT·CORS 배선이 없고,
 * 이 필터가 유일한 앱 레이어 방어다.
 *
 * <p>호출자는 order-api(예약 사가)와 shop-api(재고 조회/설정) 둘이다. {@code /actuator/**}는
 * 대상 밖 — F2의 liveness/readiness probe는 kubelet이 무인증으로 접근해야 한다.
 *
 * <p>패키지가 {@code com.minicommerce.global}인 것은 의도다. 부팅 클래스 베이스가
 * {@code com.minicommerce}라 Modulith가 {@code global}을 하나의 모듈로 보는데,
 * {@code global.security}는 비노출 하위 패키지다 — 다른 모듈(inventory)에서 참조하면
 * {@code ModularityVerificationTest}가 깨진다. shop-api/order-api의 {@code WebConfig}가
 * 같은 이유로 이 패키지에 있다.
 */
@Configuration
public class InternalSecurityConfig {

    private final String internalApiKey;

    public InternalSecurityConfig(@Value("${app.security.internal-api-key}") String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    @Bean
    public FilterRegistrationBean<InternalAuthFilter> internalAuthFilter() {
        FilterRegistrationBean<InternalAuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new InternalAuthFilter(internalApiKey));
        registrationBean.addUrlPatterns("/internal/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
