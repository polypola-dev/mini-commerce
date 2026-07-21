package com.minicommerce.order.adapter.in.event;

import com.minicommerce.inventory.InventoryReservationOversoldEvent;
import com.minicommerce.order.application.port.in.CancelOrderUseCase;
import com.minicommerce.order.domain.OrderStatus;
import com.minicommerce.order.domain.exception.OrderCancelNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * inventory 오버셀(payment-wins force-confirm이 재고 부족으로 실패)을 주문 자동 취소+환불로 잇는 order의
 * 인바운드 이벤트 어댑터. 리퍼가 풀어준 재고를 다른 주문이 이미 채가 결제 확정에 필요한 재고를 확보하지
 * 못한 경우 — inventory는 예약을 OVERSOLD로 표시하고 이 이벤트를 발행한다. order는 정직하게 주문을
 * 취소하고 기존 PG 환불 배관(OrderService.doCancel)을 그대로 재사용해 결제를 되돌린다.
 *
 * <p>order-batch 프로세스에서만 활성화한다({@code app.batch.enabled=true}) — 다른 BOOT 앱에도 order-infra가
 * 물려 있어 게이팅이 없으면 컨슈머가 여러 프로세스에서 뜬다. 취소가 거부되면({@code OrderCancelNotAllowedException})
 * 예외가 담은 현재 주문 상태로 두 경우를 구분한다: 이미 {@code CANCELED}면 Kafka 재전달로 인한 정상 멱등이라
 * WARN으로 스킵하고, 그 외 상태(SHIPPED/DELIVERED 등)면 환불이 실행되지 않았는데 자동 처리가 불가능한 돈이
 * 걸린 사고이므로 ERROR로 승격해 운영이 인지하게 한다.
 */
@Component
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true")
class InventoryReservationOversoldKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationOversoldKafkaListener.class);

    private final CancelOrderUseCase cancelOrderUseCase;

    InventoryReservationOversoldKafkaListener(CancelOrderUseCase cancelOrderUseCase) {
        this.cancelOrderUseCase = cancelOrderUseCase;
    }

    @KafkaListener(topics = "inventory.reservation.oversold", groupId = "order-batch")
    void onReservationOversold(InventoryReservationOversoldEvent event) {
        try {
            cancelOrderUseCase.cancelByAdmin(event.orderId(), "재고 소진으로 결제 확정 실패 — 자동 취소/환불");
        } catch (OrderCancelNotAllowedException e) {
            if (e.getCurrentStatus() == OrderStatus.CANCELED) {
                // 이미 취소 완료된 주문에 다시 취소 시도 — Kafka 재전달 등으로 인한 정상 멱등. 스킵.
                log.warn("inventory.reservation.oversold for already-canceled order orderId={} "
                        + "— skipping (idempotent redelivery)", event.orderId());
            } else {
                // CANCELED가 아닌 상태(SHIPPED/DELIVERED 등)에서 취소 불가 — PG 환불이 실행되지 않았는데
                // 더 이상 자동으로 처리할 수 없는 돈이 걸린 사고. 운영이 인지하도록 ERROR로 승격한다.
                log.error("inventory.reservation.oversold for order that cannot be canceled orderId={} status={} "
                        + "— REFUND NOT EXECUTED, manual intervention required", event.orderId(), e.getCurrentStatus());
            }
        }
    }
}
