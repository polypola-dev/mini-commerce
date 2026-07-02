package com.minicommerce.notification;

import com.minicommerce.order.OrderPaidEvent;
import com.minicommerce.order.OrderPlacedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * order 이벤트의 Kafka 수신 어댑터. JSON 페이로드를 이벤트 타입으로 역직렬화해
 * {@link NotificationService}에 위임한다.
 *
 * <p>Modulith externalization(shop-api {@code EventExternalizationConfig})이 order 이벤트를 JSON으로
 * {@code order.placed}/{@code order.paid} 토픽에 발행하며, spring-kafka의 JSON RecordMessageConverter가
 * 메서드 파라미터 타입으로 역직렬화한다(타입 헤더 불필요 — 선언 타입을 대상으로 사용).
 * 멱등 처리는 {@code NotificationService}가 (orderId, type) 기준으로 담당한다.
 */
@Component
class OrderEventKafkaConsumer {

    private final NotificationService notificationService;

    OrderEventKafkaConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "order.placed", groupId = "notification")
    void onOrderPlaced(OrderPlacedEvent event) {
        notificationService.on(event);
    }

    @KafkaListener(topics = "order.paid", groupId = "notification")
    void onOrderPaid(OrderPaidEvent event) {
        notificationService.on(event);
    }
}
