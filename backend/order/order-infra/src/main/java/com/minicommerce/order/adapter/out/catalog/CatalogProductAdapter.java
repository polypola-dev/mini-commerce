package com.minicommerce.order.adapter.out.catalog;

import com.minicommerce.order.application.port.out.ProductQueryPort;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * catalog를 REST로 조회한다(ADR-005 S2). order-service가 별도 프로세스로 분리돼도(S3) 그대로
 * 동작하도록, in-process {@code ProductReader} 호출 대신 catalog의 내부 API를 사용한다 —
 * 그래서 order-infra는 catalog 모듈에 컴파일 의존하지 않는다.
 */
@Component
public class CatalogProductAdapter implements ProductQueryPort {

    private record ProductInfoDto(String id, String name, BigDecimal price) {
    }

    private record OptionInfoDto(BigDecimal additionalPrice, String optionValue) {
    }

    private final RestClient catalogRestClient;

    public CatalogProductAdapter(RestClient catalogRestClient) {
        this.catalogRestClient = catalogRestClient;
    }

    @Override
    public ProductInfo findProduct(String productId) {
        ProductInfoDto dto = fetch("/internal/products/" + productId, ProductInfoDto.class, productId);
        return new ProductInfo(dto.id(), dto.name(), dto.price());
    }

    @Override
    public OptionInfo findOption(String optionId) {
        OptionInfoDto dto = fetch("/internal/products/options/" + optionId, OptionInfoDto.class, optionId);
        return new OptionInfo(dto.additionalPrice(), dto.optionValue());
    }

    private <T> T fetch(String uri, Class<T> type, String id) {
        try {
            return catalogRestClient.get().uri(uri).retrieve().body(type);
        } catch (HttpClientErrorException.NotFound e) {
            throw new EntityNotFoundException("Not found via catalog: " + id);
        }
    }
}
