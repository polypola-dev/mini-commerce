package com.minicommerce.order.application.port.out;

import java.time.Instant;
import java.util.List;

/**
 * inventory 서비스(원격, GH #3 S3)에 대한 재고 사가 포트. 예약 ID = orderId(멱등 키) —
 * reserve 재시도는 이중 차감 없이 기존 예약으로 수렴한다(계약은 inventory-api가 보장).
 */
public interface InventoryPort {
    record StockHold(String reservationId, Instant expiresAt, List<StockItem> items) {}
    record StockItem(String productId, long quantity) {}

    /** 예약 상태 스냅샷 — inventory의 원장 상태 + 원장 부재(NOT_FOUND). */
    enum ReservationState { RESERVED, CONFIRMED, RELEASED, EXPIRED, RESTOCKED, NOT_FOUND }

    /**
     * 재고 예약(동기). 품절이면 {@code OutOfStockException}, inventory 서비스 장애면
     * {@code InventoryUnavailableException}을 던진다(둘 다 order-domain 예외로 복원됨).
     */
    StockHold reserve(String orderId, List<StockItem> items);

    /** 주문 생성 실패의 보상 release(멱등). 실패해도 리퍼가 만료 시점에 백스톱으로 해제한다. */
    void release(String orderId);

    /** 결제 승인 전 사전 가드용 상태 조회 — 만료 경합 창 축소(GH #3 설계 D-B). */
    ReservationState status(String orderId);

    // confirm/restock은 order가 동기 호출하지 않는다 — order.paid/order.canceled 발행이
    // inventory-api의 코레오그래피 구독을 구동한다(GH #3 S4).
}
