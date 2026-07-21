package com.minicommerce.catalog;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * catalog→inventory-api REST 호출의 서킷브레이커 경계(D6).
 *
 * <p><b>왜 {@link InventoryClient}에서 분리했는가.</b> {@code @CircuitBreaker}는 AOP 프록시로
 * 동작하므로 같은 클래스 안의 자기호출은 프록시를 타지 않아 무효가 된다. 기존
 * {@code InventoryClient.availableStock(id, default)}는 같은 클래스의 {@code availableStocks(...)}를
 * 부르고 있었으므로, REST 호출부를 별도 빈으로 옮겨 모든 진입 경로가 프록시를 지나게 했다.
 */
@Component
public class InventoryStockGateway {

    private static final Logger log = LoggerFactory.getLogger(InventoryStockGateway.class);

    private static final ParameterizedTypeReference<Map<String, Long>> STOCK_MAP =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient inventoryRestClient;
    private final InventoryStockCache cache;

    public InventoryStockGateway(RestClient inventoryRestClient, InventoryStockCache cache) {
        this.inventoryRestClient = inventoryRestClient;
        this.cache = cache;
    }

    /**
     * 배치 조회(N+1 방지). 성공한 결과는 폴백 캐시에 적재한다 — 다음 장애 때 돌려줄 "마지막
     * 성공값"이 여기서 만들어진다.
     */
    @CircuitBreaker(name = "inventory", fallbackMethod = "fetchStocksFallback")
    public Map<String, Long> fetchStocks(List<String> productIds) {
        Map<String, Long> body = inventoryRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/inventory/stocks")
                        .queryParam("ids", productIds.toArray())
                        .build())
                .retrieve()
                .body(STOCK_MAP);
        Map<String, Long> stocks = body != null ? body : Map.of();
        cache.writeAll(stocks);
        return stocks;
    }

    /**
     * 조회 실패·타임아웃·서킷 오픈 공통 폴백. 캐시된 마지막 성공값을 쓰고, 미적중이면 0으로
     * 내려간다 — 재고를 모르는 상태에서 낙관적인 수를 보여주면 주문 단계에서 품절로 뒤집히므로
     * 보수적인 쪽(품절 표시)을 택한다.
     *
     * <p>요청한 id는 값이 없어도 반드시 키를 채워 반환한다. 호출자
     * ({@code InventoryClient.availableStock})가 {@code getOrDefault}로 상품 테이블의 stock 컬럼을
     * 쓰는데, 그 값은 원장(inventory-api)과 동기화되지 않은 낡은 수라 장애 시 폴백 대상이 아니다.
     */
    Map<String, Long> fetchStocksFallback(List<String> productIds, Throwable cause) {
        log.warn("재고 조회 실패 — 폴백 캐시로 응답한다: ids={}, cause={}",
                productIds, cause.toString());
        Map<String, Long> cached = cache.readAll(productIds);
        Map<String, Long> result = new LinkedHashMap<>();
        for (String productId : productIds) {
            result.put(productId, cached.getOrDefault(productId, 0L));
        }
        return result;
    }

    /**
     * 재고 설정(어드민). <b>폴백을 두지 않는다</b> — 쓰기가 조용히 성공한 척하면 운영자가 반영된
     * 줄 알고 넘어간다. 실패는 그대로 전파해 드러낸다.
     */
    @CircuitBreaker(name = "inventory")
    public void putStock(String productId, long stock) {
        inventoryRestClient.put()
                .uri("/internal/inventory/stock/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StockRequest(stock))
                .retrieve()
                .toBodilessEntity();
        cache.write(productId, stock);
    }

    private record StockRequest(long stock) {
    }
}
