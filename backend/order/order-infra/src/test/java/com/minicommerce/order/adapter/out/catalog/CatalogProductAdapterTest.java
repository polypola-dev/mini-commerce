package com.minicommerce.order.adapter.out.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.minicommerce.order.application.port.out.ProductQueryPort.OptionInfo;
import com.minicommerce.order.application.port.out.ProductQueryPort.ProductInfo;
import com.minicommerce.order.domain.exception.CatalogUnavailableException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CatalogProductAdapterTest {

    private MockRestServiceServer server;
    private CatalogProductAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://catalog.internal");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new CatalogProductAdapter(builder.build());
    }

    @Test
    void findProduct_returns_product_info_from_catalog_internal_api() {
        server.expect(requestTo("http://catalog.internal/internal/products/p1"))
                .andRespond(withSuccess(
                        "{\"id\":\"p1\",\"name\":\"Widget\",\"price\":1000}",
                        MediaType.APPLICATION_JSON));

        ProductInfo result = adapter.findProduct("p1");

        assertThat(result.id()).isEqualTo("p1");
        assertThat(result.name()).isEqualTo("Widget");
        assertThat(result.price()).isEqualByComparingTo("1000");
    }

    @Test
    void findProduct_whenCatalogReturns404_throwsEntityNotFoundException() {
        server.expect(requestTo("http://catalog.internal/internal/products/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.findProduct("missing"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findOption_returns_option_info_from_catalog_internal_api() {
        server.expect(requestTo("http://catalog.internal/internal/products/options/o1"))
                .andRespond(withSuccess(
                        "{\"additionalPrice\":500,\"optionValue\":\"Red\"}",
                        MediaType.APPLICATION_JSON));

        OptionInfo result = adapter.findOption("o1");

        assertThat(result.additionalPrice()).isEqualByComparingTo("500");
        assertThat(result.optionValue()).isEqualTo("Red");
    }

    @Test
    void findOption_whenCatalogReturns404_throwsEntityNotFoundException() {
        server.expect(requestTo("http://catalog.internal/internal/products/options/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.findOption("missing"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findProduct_whenCatalogUnreachable_throwsCatalogUnavailableException() {
        // 서킷이 아직 CLOSED인 상태에서의 원본 호출 실패(5xx 등)도 503 계약(CatalogUnavailableException)
        // 으로 통일돼야 한다 — CallNotPermittedException(서킷 OPEN) 전용 fallback만으로는 이 구간이
        // 새어나가 처리되지 않은 500으로 떨어졌었다(kind 클러스터 shop-api 완전 다운 재현으로 발견).
        server.expect(requestTo("http://catalog.internal/internal/products/p1"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> adapter.findProduct("p1"))
                .isInstanceOf(CatalogUnavailableException.class);
    }
}
