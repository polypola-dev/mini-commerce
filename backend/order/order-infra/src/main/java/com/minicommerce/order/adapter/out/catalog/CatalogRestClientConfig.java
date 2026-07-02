package com.minicommerce.order.adapter.out.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * catalog 내부 API 호출용 {@link RestClient}. base-url은 지금(S2, 같은 프로세스) 기본값이
 * 로컬 루프백이고, order-service가 별도 프로세스로 분리되면(S3) catalog의 실제 호스트로 바뀐다.
 */
@Configuration
class CatalogRestClientConfig {

    @Bean
    RestClient catalogRestClient(@Value("${app.catalog.base-url:http://localhost:8080}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
