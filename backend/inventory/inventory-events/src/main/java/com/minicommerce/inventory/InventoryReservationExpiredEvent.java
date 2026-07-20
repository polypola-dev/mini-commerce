package com.minicommerce.inventory;

import java.time.Instant;

/**
 * 재고 예약이 만료되어(이탈/타임아웃) 리퍼가 해제했음을 알리는 이벤트. inventory-api가
 * Kafka {@code inventory.reservation.expired}(key=orderId)로 발행하고, order-batch가 구독해
 * 주문을 EXPIRED로 전이한다(GH #3 S3 — inventory 완전분리로 in-process 이벤트에서 외부화).
 * 소비 측 멱등 근거: {@code Order.markExpired()}는 PENDING_PAYMENT일 때만 전이한다.
 */
public record InventoryReservationExpiredEvent(String reservationId, String orderId, Instant occurredAt) {
}
