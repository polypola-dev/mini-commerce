package com.minicommerce.order.adapter.in.event;

import com.minicommerce.inventory.ReservationExpiredEvent;
import com.minicommerce.order.application.port.in.ExpireOrderUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * inventory 재고 예약 만료(리퍼)를 주문 EXPIRED 전이로 잇는다. order/inventory는 서비스그룹
 * (b) 전략상 영원히 같은 프로세스이므로(ADR-005) Kafka 없이 in-process 이벤트로 충분하다.
 *
 * <p>order-batch 프로세스에서만 활성화한다({@code app.batch.enabled=true}, ADR-005 S4) — 리퍼가
 * 발행하는 이벤트를 이 프로세스가 수신하지 않으면, order-api 재시작 시 Modulith의 미발행 이벤트
 * 재발행 경로가 이 빈을 찾아 대신 처리해버리는 사고를 막을 수 있다(아키텍처 리뷰에서 지적).
 */
@Component
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true")
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
