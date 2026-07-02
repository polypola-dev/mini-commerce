package com.minicommerce.order.application.port.in;

/** 결제 대기 중 재고 예약이 만료됐을 때(이탈/타임아웃) 주문을 EXPIRED로 전이한다. */
public interface ExpireOrderUseCase {
    void expire(String orderId);
}
