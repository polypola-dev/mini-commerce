package com.minicommerce.order.adapter.out.event;

import com.minicommerce.order.OrderCanceledEvent;
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
// Issue #7 Kafka trace 전파: order-api/shop-api yml에 spring.kafka.{template,listener}.
// observation-enabled=true를 배치해 자동계측을 시도한다. 다만 이 아웃박스(event_publication)
// 발행은 원 요청 스레드가 아닌 별도 시점/스레드에서 일어나므로 발행 스레드에 trace 컨텍스트가
// 없으면 producer span이 부모 없는 root로 고아화될 수 있고, order-batch 재시도 스윕은 별도
// 프로세스라 원천적으로 전파 불가하다. 자동계측으로 안 붙으면 수동 traceparent 전파는 하지
// 않는다(계획 F2로 분리). 런타임 producer→consumer traceId 공유 여부는 docker-compose 통합
// 검증 단계에서 확인.
@Configuration
public class EventExternalizationConfig {

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .select(event -> event instanceof OrderPlacedEvent
                        || event instanceof OrderPaidEvent
                        || event instanceof OrderCanceledEvent)
                .route(OrderPlacedEvent.class,
                        event -> RoutingTarget.forTarget("order.placed").andKey(event.orderId()))
                .route(OrderPaidEvent.class,
                        event -> RoutingTarget.forTarget("order.paid").andKey(event.orderId()))
                .route(OrderCanceledEvent.class,
                        event -> RoutingTarget.forTarget("order.canceled").andKey(event.orderId()))
                .build();
    }
}
