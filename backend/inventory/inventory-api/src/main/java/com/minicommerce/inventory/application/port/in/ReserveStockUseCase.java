package com.minicommerce.inventory.application.port.in;

import java.util.List;

/**
 * 주문 재고 예약(분산 사가의 동기 구간). 멱등 키 = orderId — 재시도는 기존 RESERVED 예약을
 * 성공으로 수렴시킨다(이중 차감 없음, InventoryService.reserveForOrder 참고).
 */
public interface ReserveStockUseCase {

    record ReserveCommand(String orderId, List<Item> items) {
        public record Item(String productId, long quantity) {
        }
    }

    record ReserveResult(ReservationView view, boolean created) {
    }

    ReserveResult reserve(ReserveCommand command);
}
