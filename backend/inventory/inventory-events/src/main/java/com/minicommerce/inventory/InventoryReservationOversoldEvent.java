package com.minicommerce.inventory;

import java.time.Instant;

/**
 * 결제가 이긴 경합(payment-wins force-confirm)에서 재고가 이미 다른 주문에 채여 강제 확정이 불가능함을
 * 알리는 이벤트(오버셀). inventory-api가 Kafka {@code inventory.reservation.oversold}(key=orderId)로
 * 발행하고, order-batch가 구독해 주문을 자동 취소+환불한다 — 재고를 채우지 못했으니 정직하게 결제를
 * 되돌린다. 소비 측 멱등 근거: 이미 CANCELED인 주문 재취소는 {@code OrderCancelNotAllowedException}로 스킵한다.
 */
public record InventoryReservationOversoldEvent(String reservationId, String orderId, Instant occurredAt) {
}
