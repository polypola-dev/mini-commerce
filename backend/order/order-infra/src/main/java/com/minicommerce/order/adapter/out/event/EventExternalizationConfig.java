package com.minicommerce.order.adapter.out.event;

import com.minicommerce.order.OrderPaidEvent;
import com.minicommerce.order.OrderPlacedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * order 도메인 이벤트를 Kafka로 externalize 하는 라우팅 설정.
 *
 * <p>도메인 이벤트(order-domain)에 {@code @Externalized}를 직접 붙이면 도메인이 Kafka 토픽을
 * 알게 되므로, 대신 인프라 계층에서 프로그래밍 방식으로 라우팅한다 — 도메인 순수성 유지.
 * {@code event_publication} 테이블이 아웃박스 역할을 하며, order-api(발행)와 order-batch(미발행분
 * 재시도 스윕, ADR-005 S4) 양쪽 부팅 앱이 동일한 라우팅 규칙을 알아야 하므로 order-infra에 둔다.
 */
@Configuration
public class EventExternalizationConfig {

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .select(event -> event instanceof OrderPlacedEvent || event instanceof OrderPaidEvent)
                .route(OrderPlacedEvent.class,
                        event -> RoutingTarget.forTarget("order.placed").andKey(event.orderId()))
                .route(OrderPaidEvent.class,
                        event -> RoutingTarget.forTarget("order.paid").andKey(event.orderId()))
                .build();
    }
}
