package com.minicommerce.inventory.adapter.in.event;

import com.minicommerce.inventory.application.port.in.SettleReservationUseCase;
import com.minicommerce.order.OrderCanceledEvent;
import com.minicommerce.order.OrderPaidEvent;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * order 결제/취소 이벤트를 재고 확정/재입고로 잇는 Kafka 수신 어댑터(GH #3 S4 코레오그래피).
 * S3의 동기 REST(confirm/restock)를 대체한다 — order는 order.paid/order.canceled를 발행만 하고,
 * inventory-api가 구독해 자기 원장/Redis를 조율한다. order-events 계약 모듈에 의존한다(발행자
 * order-infra·구독자 shop-api notification과 동일 방향 — 계약 소비이지 구현 의존이 아니다).
 *
 * <p>멱등/경합: confirm은 CONFIRMED 멱등·리퍼 경합 시 payment-wins force-confirm, restock은
 * RESTOCKED 멱등을 InventoryService가 보장한다. 예약 원장이 아예 없으면(구주문 등) 재시도해도
 * 소용없으므로 WARN 후 스킵한다. restock이 아직 CONFIRMED가 아닌 예약을 만나면(취소가 결제
 * 확정보다 먼저 소비된 드문 순서) IllegalStateException을 전파해 Kafka 재시도로 수렴시킨다.
 */
@Component
class OrderEventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventKafkaConsumer.class);

    private final SettleReservationUseCase settleReservationUseCase;

    OrderEventKafkaConsumer(SettleReservationUseCase settleReservationUseCase) {
        this.settleReservationUseCase = settleReservationUseCase;
    }

    @KafkaListener(topics = "order.paid", groupId = "inventory")
    void onOrderPaid(OrderPaidEvent event) {
        try {
            settleReservationUseCase.confirm(event.orderId());
        } catch (EntityNotFoundException e) {
            log.warn("order.paid for unknown reservation orderId={} — skipping confirm", event.orderId());
        }
    }

    @KafkaListener(topics = "order.canceled", groupId = "inventory")
    void onOrderCanceled(OrderCanceledEvent event) {
        try {
            settleReservationUseCase.restock(event.orderId());
        } catch (EntityNotFoundException e) {
            log.warn("order.canceled for unknown reservation orderId={} — skipping restock", event.orderId());
        }
    }
}
