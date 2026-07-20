package com.minicommerce.inventory.application.port.in;

/**
 * 결제 확정(confirm)/취소 재입고(restock) — S3 한시 동기 REST 경로. S4에서 order.paid/
 * order.canceled Kafka 구독(코레오그래피)으로 대체하고 이 유즈케이스와 해당 엔드포인트를 삭제한다.
 */
public interface SettleReservationUseCase {

    void confirm(String orderId);

    void restock(String orderId);
}
