package com.minicommerce.order.adapter.out.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * 토스페이먼츠 결제 승인 API 호출용 {@link RestClient}. 시크릿 키는 Basic 인증에 base64(secretKey + ":")로 싣는다.
 */
@Configuration
class TossPaymentConfig {

    // 정적 RestClient.builder() 대신 오토컨피그된 RestClient.Builder 빈을 주입받아야
    // ObservationRestClientCustomizer가 적용돼 분산 트레이스 자동계측이 붙는다(Issue #7).
    // 기본값을 빈 문자열로 둔다: 이 config는 order-infra에 있어 order-admin/order-batch도
    // 전이 스캔해 OrderService 조립에 필요하지만 그들은 결제를 호출하지 않는다 — 실 키는
    // order-api에만 env로 주입된다(REDIS_PASSWORD와 동일한 선례).
    @Bean
    RestClient tossRestClient(RestClient.Builder builder,
                              @Value("${TOSS_SECRET_KEY:}") String secretKey) {
        String basicAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return builder
                .baseUrl("https://api.tosspayments.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                .build();
    }
}
