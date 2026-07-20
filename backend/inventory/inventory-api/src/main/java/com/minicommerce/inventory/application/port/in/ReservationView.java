package com.minicommerce.inventory.application.port.in;

import java.time.Instant;

/** 예약 조회/생성 응답 공용 뷰. reservationId = orderId(1주문 1예약, GH #3 S3). */
public record ReservationView(String reservationId, String orderId, String status, Instant expiresAt) {
}
