package com.minicommerce.inventory;

/**
 * 같은 orderId의 예약이 이미 RESERVED 외 상태(RELEASED/EXPIRED/CONFIRMED/RESTOCKED)라 재예약할 수
 * 없을 때 발생한다(GH #3 S3). 정상 재시도(RESERVED)는 멱등 성공이므로 이 예외가 나오지 않는다 —
 * 이 예외는 만료 후 재시도 같은 비정상 흐름의 신호다. inventory-api가 409(reservation-conflict)로 매핑한다.
 */
public class ReservationConflictException extends RuntimeException {

    private final ReservationStatus status;

    public ReservationConflictException(String orderId, ReservationStatus status) {
        super("Reservation for order " + orderId + " is not re-reservable in status: " + status);
        this.status = status;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
