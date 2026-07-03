package com.minicommerce.catalog;

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

    @Bean
    RestClient inventoryRestClient(@Value("${app.inventory.base-url:http://localhost:8081}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
