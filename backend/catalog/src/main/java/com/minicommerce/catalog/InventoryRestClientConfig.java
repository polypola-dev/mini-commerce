package com.minicommerce.catalog;

import com.minicommerce.global.InternalApiContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * order-api의 재고 내부 API 호출용 {@link RestClient}(ADR-005 S3-3b). order-infra의
 * {@code CatalogRestClientConfig}와 대칭이며, base-url 기본값은 order-api의 로컬 포트(8081)를
 * 가리킨다. order-api가 별도 호스트로 배포되면 {@code app.inventory.base-url}로 교체한다.
 */
@Configuration
class InventoryRestClientConfig {

    // 정적 RestClient.builder() 대신 오토컨피그된 RestClient.Builder 빈을 주입받아야
    // ObservationRestClientCustomizer가 적용돼 분산 트레이스 자동계측이 붙는다(Issue #7).
    @Bean
    RestClient inventoryRestClient(RestClient.Builder builder,
                                   @Value("${app.inventory.base-url:http://localhost:8081}") String baseUrl,
                                   @Value("${app.security.internal-api-key}") String internalApiKey) {
        // 서비스간 인증 헤더(B3, ADR-020) — 수신측 inventory-api의 InternalAuthFilter가 검증한다.
        return builder.baseUrl(baseUrl)
                .defaultHeader(InternalApiContract.INTERNAL_KEY_HEADER, internalApiKey)
                .build();
    }
}
