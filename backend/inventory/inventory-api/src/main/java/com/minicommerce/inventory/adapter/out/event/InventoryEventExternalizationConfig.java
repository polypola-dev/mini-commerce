package com.minicommerce.inventory.adapter.out.event;

import com.minicommerce.inventory.InventoryReservationExpiredEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * inventory 이벤트를 Kafka로 externalize 하는 라우팅 설정(GH #3 S3). order-infra
 * EventExternalizationConfig와 동일 패턴 — 이벤트 계약(inventory-events)에 토픽을 심지 않고
 * 인프라 계층에서 프로그래밍 방식으로 라우팅한다. event_publication(inventorydb)이 아웃박스.
 * 미발행 재시도는 별도 스위퍼 대신 republish-outstanding-events-on-restart(application.yml)로
 * 처리한다 — 단일 컨슈머(order-batch expire)가 멱등이라 재발행이 안전하다.
 * (부채: 레플리카 2+로 스케일 시 order-batch식 스위퍼 재검토 — architecture/inventory.md)
 */
@Configuration
public class InventoryEventExternalizationConfig {

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .select(event -> event instanceof InventoryReservationExpiredEvent)
                .route(InventoryReservationExpiredEvent.class,
                        event -> RoutingTarget.forTarget("inventory.reservation.expired").andKey(event.orderId()))
                .build();
    }
}
