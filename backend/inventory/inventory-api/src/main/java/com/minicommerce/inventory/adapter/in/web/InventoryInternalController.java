package com.minicommerce.inventory.adapter.in.web;

import com.minicommerce.inventory.application.port.in.GetStocksUseCase;
import com.minicommerce.inventory.application.port.in.SetStockUseCase;
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
 * catalog(shop-api)가 REST로 재고를 조회/설정하는 내부 전용 API. order-api에서 이관(GH #3 S2) —
 * inventory가 독립 서비스가 되면서 이 엔드포인트의 네트워크 경계가 order-api에서 inventory-api로
 * 옮겨졌다(경로 계약 불변, 호출자는 INVENTORY_BASE_URL만 전환). ingress에는 비노출(/internal).
 */
@RestController
@RequestMapping("/internal/inventory")
class InventoryInternalController {

    private final GetStocksUseCase getStocksUseCase;
    private final SetStockUseCase setStockUseCase;

    InventoryInternalController(GetStocksUseCase getStocksUseCase, SetStockUseCase setStockUseCase) {
        this.getStocksUseCase = getStocksUseCase;
        this.setStockUseCase = setStockUseCase;
    }

    /** 배치 조회. {@code ?ids=a,b,c} → {productId: availableStock} Map(N+1 방지). */
    @GetMapping("/stocks")
    Map<String, Long> availableStocks(@RequestParam(name = "ids", required = false) List<String> ids) {
        return getStocksUseCase.availableStocks(ids);
    }

    @PutMapping("/stock/{productId}")
    long setStock(@PathVariable String productId, @RequestBody StockRequest request) {
        return setStockUseCase.setStock(productId, request.stock());
    }

    record StockRequest(long stock) {
    }
}
