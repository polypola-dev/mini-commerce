package com.minicommerce.order.adapter.out.inventory;

import com.minicommerce.inventory.InventoryHold;
import com.minicommerce.inventory.InventoryItem;
import com.minicommerce.inventory.InventoryService;
import com.minicommerce.order.application.port.out.InventoryPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InventoryAdapter implements InventoryPort {

    private final InventoryService inventoryService;

    public InventoryAdapter(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public StockHold reserve(List<StockItem> items) {
        List<InventoryItem> inventoryItems = items.stream()
                .map(i -> new InventoryItem(i.productId(), i.quantity()))
                .toList();
        InventoryHold hold = inventoryService.reserve(inventoryItems);
        return new StockHold(hold.reservationId(), hold.expiresAt(), items);
    }

    @Override
    public void release(StockHold hold) {
        List<InventoryItem> inventoryItems = hold.items().stream()
                .map(i -> new InventoryItem(i.productId(), i.quantity()))
                .toList();
        inventoryService.release(new InventoryHold(hold.reservationId(), hold.expiresAt(), inventoryItems));
    }

    @Override
    public void createReservationForOrder(String orderId, StockHold hold) {
        List<InventoryItem> items = hold.items().stream()
                .map(i -> new InventoryItem(i.productId(), i.quantity()))
                .toList();
        inventoryService.createReservationForOrder(orderId, hold.reservationId(), hold.expiresAt(), items);
    }

    @Override
    public void confirmByOrderId(String orderId) {
        inventoryService.confirmByOrderId(orderId);
    }

    @Override
    public void restockByOrderId(String orderId) {
        inventoryService.restockByOrderId(orderId);
    }
}
