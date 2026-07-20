package com.minicommerce.order.adapter.out.inventory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * inventory-api 예약 사가 호출용 {@link RestClient}(GH #3 S3). CatalogRestClientConfig와 동일하게
 * 오토컨피그된 Builder를 주입받아 분산 트레이스 자동계측을 유지한다. catalog와 달리 타임아웃을
 * 명시한다 — reserve/confirm이 사용자 요청 경로에 있어 inventory-api가 응답 없이 매달리면 주문
 * 전체가 매달린다(기본 read timeout은 무한). 타임아웃 후 재시도는 멱등 키(orderId)로 안전하다.
 */
@Configuration
class InventoryRestClientConfig {

    @Bean
    RestClient inventorySagaRestClient(RestClient.Builder builder,
                                       @Value("${app.inventory.base-url:http://localhost:8084}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(5_000);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
