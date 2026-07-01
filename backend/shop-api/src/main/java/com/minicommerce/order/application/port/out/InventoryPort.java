package com.minicommerce.order.application.port.out;

import java.time.Instant;
import java.util.List;

public interface InventoryPort {
    record StockHold(String reservationId, Instant expiresAt, List<StockItem> items) {}
    record StockItem(String productId, long quantity) {}

    StockHold reserve(List<StockItem> items);
    void release(StockHold hold);
    void createReservationForOrder(String orderId, StockHold hold);
    void confirmByOrderId(String orderId);
}
