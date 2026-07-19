package com.minicommerce.inventory;

/**
 * 재고 예약이 만료되어(이탈/타임아웃) 해제됐음을 알리는 도메인 이벤트.
 * order가 소비해 해당 주문을 EXPIRED로 전이한다. order/inventory는 서비스그룹 (b) 전략상
 * 영원히 같은 프로세스이므로(ADR-005) in-process 이벤트로 충분하며 Kafka로 외부화하지 않는다.
 */
public record ReservationExpiredEvent(String reservationId, String orderId) {
}
