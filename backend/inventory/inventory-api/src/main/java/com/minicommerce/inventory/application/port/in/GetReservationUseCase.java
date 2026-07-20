package com.minicommerce.inventory.application.port.in;

import java.util.Optional;

/** 예약 상태 조회 — order-api가 결제 승인 전 사전 가드(만료 경합 창 축소)로 사용한다. */
public interface GetReservationUseCase {

    Optional<ReservationView> get(String orderId);
}
