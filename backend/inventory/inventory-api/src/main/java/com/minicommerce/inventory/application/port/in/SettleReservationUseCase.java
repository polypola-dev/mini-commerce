package com.minicommerce.inventory.application.port.in;

/**
 * 결제 확정(confirm)/취소 재입고(restock). GH #3 S4 코레오그래피에서 order.paid/order.canceled
 * Kafka 구독({@code OrderEventKafkaConsumer})이 이 유즈케이스를 구동한다(S3의 동기 REST를 대체).
 * 멱등·경합 처리는 inventory-core InventoryService가 담당한다(confirm의 payment-wins 등).
 */
public interface SettleReservationUseCase {

    void confirm(String orderId);

    void restock(String orderId);
}
