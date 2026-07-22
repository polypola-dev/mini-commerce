package com.minicommerce.inventory.application;

import com.minicommerce.inventory.InventoryHold;
import com.minicommerce.inventory.InventoryItem;
import com.minicommerce.inventory.InventoryService;
import com.minicommerce.inventory.application.port.in.GetReservationUseCase;
import com.minicommerce.inventory.application.port.in.ReleaseReservationUseCase;
import com.minicommerce.inventory.application.port.in.ReservationView;
import com.minicommerce.inventory.application.port.in.ReserveStockUseCase;
import com.minicommerce.inventory.application.port.in.SettleReservationUseCase;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 예약 사가 유즈케이스. 레거시 {@link InventoryService}(inventory-core)에 위임한다(최소 변경 결정,
 * StockService와 동일). 트랜잭션을 열지 않는 것이 계약이다 — reserveForOrder는 "원장 커밋 → Redis
 * Lua" 순서를 전제하므로 바깥 트랜잭션이 있으면 커밋이 Lua 뒤로 밀려 크래시 창 방향이 뒤집힌다.
 */
@Service
public class ReservationService
        implements ReserveStockUseCase, ReleaseReservationUseCase, GetReservationUseCase, SettleReservationUseCase {

    private final InventoryService inventoryService;

    public ReservationService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public ReserveResult reserve(ReserveCommand command) {
        // created 판정은 응답 코드(201/200)용 — 경합 시 부정확해도 무해하다.
        boolean existed = inventoryService.getByOrderId(command.orderId()).isPresent();
        List<InventoryItem> items = command.items().stream()
                .map(item -> new InventoryItem(item.productId(), item.quantity()))
                .toList();
        InventoryHold hold = inventoryService.reserveForOrder(command.orderId(), items);
        return new ReserveResult(
                new ReservationView(hold.reservationId(), command.orderId(), "RESERVED", hold.expiresAt()),
                !existed);
    }

    @Override
    public void release(String orderId) {
        inventoryService.releaseByOrderId(orderId);
    }

    @Override
    public Optional<ReservationView> get(String orderId) {
        return inventoryService.getByOrderId(orderId)
                .map(reservation -> new ReservationView(
                        reservation.getId().toString(), reservation.getOrderId().toString(),
                        reservation.getStatus().name(), reservation.getExpiresAt()));
    }

    @Override
    public void confirm(String orderId) {
        inventoryService.confirmByOrderId(orderId);
    }

    @Override
    public void restock(String orderId) {
        inventoryService.restockByOrderId(orderId);
    }
}
