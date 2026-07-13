package com.minicommerce.catalog;

import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * catalog가 재고를 order-api 내부 API로 조회/설정하는 어댑터(ADR-005 S3-3b). 기존 in-process
 * {@code InventoryService} 직접 호출을 대체하며, order-api가 별도 프로세스로 분리돼도 그대로
 * 동작한다 — 그래서 catalog는 inventory 모듈에 컴파일 의존하지 않는다.
 */
@Component
public class InventoryClient {

    private static final ParameterizedTypeReference<Map<String, Long>> STOCK_MAP =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient inventoryRestClient;

    public InventoryClient(RestClient inventoryRestClient) {
        this.inventoryRestClient = inventoryRestClient;
    }

    /**
     * 배치 조회(N+1 방지). productId 목록을 한 번의 요청으로 {productId: availableStock} Map으로
     * 받는다. 목록이 비어 있으면 요청 없이 빈 Map을 반환한다.
     */
    public Map<String, Long> availableStocks(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Long> body = inventoryRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/inventory/stocks")
                        .queryParam("ids", productIds.toArray())
                        .build())
                .retrieve()
                .body(STOCK_MAP);
        return body != null ? body : Map.of();
    }

    public long availableStock(String productId, long defaultStock) {
        return availableStocks(List.of(productId)).getOrDefault(productId, defaultStock);
    }

    public void setStock(String productId, long stock) {
        inventoryRestClient.put()
                .uri("/internal/inventory/stock/{productId}", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new StockRequest(stock))
                .retrieve()
                .toBodilessEntity();
    }

    private record StockRequest(long stock) {
    }
}
