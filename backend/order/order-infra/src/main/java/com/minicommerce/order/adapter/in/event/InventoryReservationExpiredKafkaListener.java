package com.minicommerce.order.adapter.in.event;

import com.minicommerce.inventory.InventoryReservationExpiredEvent;
import com.minicommerce.order.application.port.in.ExpireOrderUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * inventory 재고 예약 만료를 주문 EXPIRED 전이로 잇는 order의 인바운드 이벤트 어댑터(GH #3 S3).
 * inventory 완전분리로 과거 in-process ReservationExpiredEventListener를 Kafka 구독으로 대체했다 —
 * 자리(order 모듈의 adapter/in/event)는 그대로다. 이벤트 타입은 inventory-events 계약 모듈의
 * 루트 패키지 공개 타입이라 Modulith 모듈 경계를 위반하지 않는다(구 in-process 이벤트와 동일 구조).
 *
 * <p>order-batch 프로세스에서만 활성화한다({@code app.batch.enabled=true}) — order-api/order-admin도
 * order-infra를 물고 있어 게이팅이 없으면 컨슈머가 3개 프로세스에서 뜬다(그쪽엔 consumer 역직렬화
 * 설정도 없다). 재전달 멱등 근거: {@code Order.markExpired()}는 PENDING_PAYMENT일 때만 전이한다.
 */
@Component
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true")
class InventoryReservationExpiredKafkaListener {

    private final ExpireOrderUseCase expireOrderUseCase;

    InventoryReservationExpiredKafkaListener(ExpireOrderUseCase expireOrderUseCase) {
        this.expireOrderUseCase = expireOrderUseCase;
    }

    @KafkaListener(topics = "inventory.reservation.expired", groupId = "order-batch")
    void onReservationExpired(InventoryReservationExpiredEvent event) {
        expireOrderUseCase.expire(event.orderId());
    }
}
