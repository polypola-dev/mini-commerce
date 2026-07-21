package com.minicommerce.catalog;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * catalog가 재고를 order-api 내부 API로 조회/설정하는 어댑터(ADR-005 S3-3b). 기존 in-process
 * {@code InventoryService} 직접 호출을 대체하며, order-api가 별도 프로세스로 분리돼도 그대로
 * 동작한다 — 그래서 catalog는 inventory 모듈에 컴파일 의존하지 않는다.
 *
 * <p>실제 REST 호출과 서킷브레이커·폴백은 {@link InventoryStockGateway}가 맡는다(D6). 이 클래스는
 * 컨트롤러가 쓰는 공개 시그니처를 유지하는 얇은 위임 계층이다.
 */
@Component
public class InventoryClient {

    private final InventoryStockGateway gateway;

    public InventoryClient(InventoryStockGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * 배치 조회(N+1 방지). productId 목록을 한 번의 요청으로 {productId: availableStock} Map으로
     * 받는다. 목록이 비어 있으면 요청 없이 빈 Map을 반환한다.
     */
    public Map<String, Long> availableStocks(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return gateway.fetchStocks(productIds);
    }

    public long availableStock(String productId, long defaultStock) {
        return availableStocks(List.of(productId)).getOrDefault(productId, defaultStock);
    }

    public void setStock(String productId, long stock) {
        gateway.putStock(productId, stock);
    }
}
