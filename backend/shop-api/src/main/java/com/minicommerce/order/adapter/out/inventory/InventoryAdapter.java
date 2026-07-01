package com.minicommerce.order.adapter.out.inventory;

import com.minicommerce.inventory.InventoryHold;
import com.minicommerce.inventory.InventoryItem;
import com.minicommerce.inventory.InventoryReservation;
import com.minicommerce.inventory.InventoryReservationRepository;
import com.minicommerce.inventory.InventoryService;
import com.minicommerce.inventory.ReservationLine;
import com.minicommerce.order.application.port.out.InventoryPort;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InventoryAdapter implements InventoryPort {

    private final InventoryService inventoryService;
    private final InventoryReservationRepository reservationRepository;

    public InventoryAdapter(InventoryService inventoryService, InventoryReservationRepository reservationRepository) {
        this.inventoryService = inventoryService;
        this.reservationRepository = reservationRepository;
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
        List<ReservationLine> lines = hold.items().stream()
                .map(i -> new ReservationLine(i.productId(), i.quantity()))
                .toList();
        reservationRepository.save(new InventoryReservation(hold.reservationId(), orderId, hold.expiresAt(), lines));
    }

    @Override
    public void confirmByOrderId(String orderId) {
        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found for order: " + orderId));
        reservation.confirm();
        inventoryService.confirm(reservation.getId());
    }
}
