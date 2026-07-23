package com.minicommerce.order.adapter.out.catalog;

import com.minicommerce.order.application.port.out.ProductQueryPort;
import com.minicommerce.order.domain.exception.CatalogUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * catalog를 REST로 조회한다(ADR-005 S2). order-service가 별도 프로세스로 분리돼도(S3) 그대로
 * 동작하도록, in-process {@code ProductReader} 호출 대신 catalog의 내부 API를 사용한다 —
 * 그래서 order-infra는 catalog 모듈에 컴파일 의존하지 않는다.
 *
 * <p><b>서킷브레이커(D6)</b>: 폴백 없이 빠르게 실패한다 — 주문 금액 계산의 입력이라 낡거나 추측한
 * 가격으로 진행하면 잘못된 금액이 확정된다. 서킷이 열린 동안에는 호출 자체가 차단되고
 * {@link CatalogUnavailableException}(503)으로 매핑된다.
 *
 * <p>404(없는 상품)는 서킷 실패로 세지 않는다 — 정상 비즈니스 응답이다. 어댑터가 이를
 * {@code EntityNotFoundException}으로 바꾸므로 설정의 {@code ignoreExceptions}에 그 타입을
 * 등록해 둔다. 폴백 메서드도 {@link CallNotPermittedException}만 받게 좁혀서, 무시 대상 예외가
 * 폴백에 삼켜져 503으로 둔갑하지 않게 한다.
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
    @CircuitBreaker(name = "catalog", fallbackMethod = "findProductFallback")
    public ProductInfo findProduct(String productId) {
        ProductInfoDto dto = fetch("/internal/products/" + productId, ProductInfoDto.class, productId);
        return new ProductInfo(dto.id(), dto.name(), dto.price());
    }

    ProductInfo findProductFallback(String productId, CallNotPermittedException cause) {
        throw new CatalogUnavailableException("Catalog circuit open for product " + productId, cause);
    }

    @Override
    @CircuitBreaker(name = "catalog", fallbackMethod = "findOptionFallback")
    public OptionInfo findOption(String optionId) {
        OptionInfoDto dto = fetch("/internal/products/options/" + optionId, OptionInfoDto.class, optionId);
        return new OptionInfo(dto.additionalPrice(), dto.optionValue());
    }

    OptionInfo findOptionFallback(String optionId, CallNotPermittedException cause) {
        throw new CatalogUnavailableException("Catalog circuit open for option " + optionId, cause);
    }

    private <T> T fetch(String uri, Class<T> type, String id) {
        try {
            return catalogRestClient.get().uri(uri).retrieve().body(type);
        } catch (HttpClientErrorException.NotFound e) {
            throw new EntityNotFoundException("Not found via catalog: " + id);
        } catch (RestClientException e) {
            // 서킷이 아직 CLOSED인 상태에서의 원본 호출 실패(연결 거부/타임아웃 등)도 503으로
            // 통일한다 — CallNotPermittedException(서킷 OPEN)만 잡는 fallback으로는 이 구간이
            // 새어나가 500으로 떨어졌다(실측으로 발견). ignoreExceptions에 없으므로 서킷 실패
            // 통계는 그대로 반영된다.
            throw new CatalogUnavailableException("Catalog call failed for " + id, e);
        }
    }
}
