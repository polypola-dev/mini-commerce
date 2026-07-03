package com.minicommerce.order.adapter.out.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * catalog 내부 API 호출용 {@link RestClient}. order-api가 별도 프로세스로 분리된 지금(ADR-005
 * S3-3b) base-url은 catalog(shop-api)의 실제 호스트를 가리킨다(docker-compose 배선).
 */
@Configuration
class CatalogRestClientConfig {

    @Bean
    RestClient catalogRestClient(@Value("${app.catalog.base-url:http://localhost:8080}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
