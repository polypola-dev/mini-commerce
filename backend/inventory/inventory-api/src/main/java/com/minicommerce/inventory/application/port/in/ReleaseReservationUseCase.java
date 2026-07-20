package com.minicommerce.inventory.application.port.in;

/** 예약 해제(주문 생성 실패의 보상 경로). 없는 예약·이미 해제된 예약도 성공으로 수렴한다(멱등). */
public interface ReleaseReservationUseCase {

    void release(String orderId);
}
