package com.minicommerce.inventory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * catalog(shop-api 잔류)가 REST로 재고를 조회/설정하는 내부 전용 API(ADR-005 S3-3b).
 * order-api가 별도 프로세스로 분리되면서 이 엔드포인트가 재고의 실제 네트워크 경계가 된다.
 * order-infra→catalog 방향의 {@code ProductInternalController}와 대칭으로, catalog→order-api
 * (재고) 방향을 담당한다.
 */
@RestController
@RequestMapping("/internal/inventory")
class InventoryInternalController {

    private final InventoryService inventoryService;

    InventoryInternalController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * 배치 조회. {@code ?ids=a,b,c} 형태의 productId 목록을 받아 {productId: availableStock} Map을
     * 반환한다(N+1 방지). 요청 쪽은 자기 DB의 seed 기본값을 전달하지 않으므로 레디스 미존재 시
     * fallback은 0으로 고정한다 — 실제 초기값은 catalog가 생성 시점에 setStock으로 넣는다.
     */
    @GetMapping("/stocks")
    Map<String, Long> availableStocks(@RequestParam(name = "ids", required = false) List<String> ids) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            result.put(id, inventoryService.availableStock(id, 0L));
        }
        return result;
    }

    @PutMapping("/stock/{productId}")
    long setStock(@PathVariable String productId, @RequestBody StockRequest request) {
        inventoryService.setStock(productId, request.stock());
        return inventoryService.availableStock(productId, request.stock());
    }

    record StockRequest(long stock) {
    }
}
