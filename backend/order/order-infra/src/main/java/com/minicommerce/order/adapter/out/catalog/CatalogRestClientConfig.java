package com.minicommerce.order.adapter.out.catalog;

import com.minicommerce.global.InternalApiContract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * catalog 내부 API 호출용 {@link RestClient}. order-api가 별도 프로세스로 분리된 지금(ADR-005
 * S3-3b) base-url은 catalog(shop-api)의 실제 호스트를 가리킨다(docker-compose 배선).
 *
 * <p>타임아웃(D6): 상품/옵션 조회는 주문 생성 경로의 첫 단계라 catalog가 응답 없이 매달리면
 * 주문 전체가 매달린다(기본 read timeout은 무한). 재고 예약보다 앞선 읽기 전용 호출이라
 * 재시도·보상 부담이 없어 read는 3s로 끊는다.
 */
@Configuration
class CatalogRestClientConfig {

    // 정적 RestClient.builder() 대신 오토컨피그된 RestClient.Builder 빈을 주입받아야
    // ObservationRestClientCustomizer가 적용돼 분산 트레이스 자동계측이 붙는다(Issue #7).
    @Bean
    RestClient catalogRestClient(RestClient.Builder builder,
                                 @Value("${app.catalog.base-url:http://localhost:8080}") String baseUrl,
                                 @Value("${app.security.internal-api-key}") String internalApiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(3_000);
        // 서비스간 인증 헤더(B3, ADR-020) — 수신측 shop-api의 InternalAuthFilter가 검증한다.
        return builder.baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(InternalApiContract.INTERNAL_KEY_HEADER, internalApiKey)
                .build();
    }
}
