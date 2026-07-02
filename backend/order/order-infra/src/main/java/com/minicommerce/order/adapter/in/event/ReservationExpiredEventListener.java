package com.minicommerce.order.adapter.in.event;

import com.minicommerce.inventory.ReservationExpiredEvent;
import com.minicommerce.order.application.port.in.ExpireOrderUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * inventory 재고 예약 만료(리퍼)를 주문 EXPIRED 전이로 잇는다. order/inventory는 서비스그룹
 * (b) 전략상 영원히 같은 프로세스이므로(ADR-005) Kafka 없이 in-process 이벤트로 충분하다.
 */
@Component
class ReservationExpiredEventListener {

    private final ExpireOrderUseCase expireOrderUseCase;

    ReservationExpiredEventListener(ExpireOrderUseCase expireOrderUseCase) {
        this.expireOrderUseCase = expireOrderUseCase;
    }

    @ApplicationModuleListener
    void on(ReservationExpiredEvent event) {
        expireOrderUseCase.expire(event.orderId());
    }
}
