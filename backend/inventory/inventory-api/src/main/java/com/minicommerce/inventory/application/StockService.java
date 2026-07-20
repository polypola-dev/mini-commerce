package com.minicommerce.inventory.application;

import com.minicommerce.inventory.InventoryService;
import com.minicommerce.inventory.application.port.in.GetStocksUseCase;
import com.minicommerce.inventory.application.port.in.SetStockUseCase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 재고 조회/설정 유즈케이스. 레거시 {@link InventoryService}(inventory-core, 플랫 구조)에 위임한다 —
 * 내부 전환은 GH #3 범위 밖(최소 변경 결정), 새로 추가되는 계층(port/in·adapter)만 헥사고날 규칙을 따른다.
 */
@Service
public class StockService implements GetStocksUseCase, SetStockUseCase {

    private final InventoryService inventoryService;

    public StockService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public Map<String, Long> availableStocks(List<String> productIds) {
        Map<String, Long> result = new LinkedHashMap<>();
        if (productIds == null) {
            return result;
        }
        for (String id : productIds) {
            if (id == null || id.isBlank()) {
                continue;
            }
            result.put(id, inventoryService.availableStock(id, 0L));
        }
        return result;
    }

    @Override
    public long setStock(String productId, long stock) {
        inventoryService.setStock(productId, stock);
        return inventoryService.availableStock(productId, stock);
    }
}
