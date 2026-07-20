package com.minicommerce.order.domain.exception;

/**
 * 결제 승인 전 사전 가드(GH #3 설계 D-B)에서 재고 예약이 유효하지 않을 때(RELEASED/EXPIRED/
 * RESTOCKED/원장 부재) 발생한다 — 리퍼가 이미 해제한 예약이면 PG 승인 전에 거절해 만료↔결제
 * 경합 창을 좁힌다. CONFIRMED는 이전 시도가 재고 확정까지 마친 재시도 경로라 가드를 통과시킨다.
 */
public class ReservationNotActiveException extends RuntimeException {
    public ReservationNotActiveException(String orderId, String state) {
        super("Reservation for order " + orderId + " is not active: " + state);
    }
}
