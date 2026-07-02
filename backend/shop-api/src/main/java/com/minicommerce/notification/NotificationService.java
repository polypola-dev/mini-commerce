package com.minicommerce.notification;

import com.minicommerce.order.OrderPaidEvent;
import com.minicommerce.order.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * order 이벤트를 받아 알림을 생성/발송한다.
 *
 * <p>이벤트 수신 경로는 in-process 리스너가 아니라 Kafka({@link OrderEventKafkaConsumer})다 —
 * order-service가 별도 프로세스로 분리돼도 이벤트를 받기 위함(ADR-005). Kafka는 at-least-once라
 * {@code (orderId, type)} 기준 멱등 가드를 둔다.
 */
@Service
class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationSender sender;

    NotificationService(NotificationRepository repository, NotificationSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    void on(OrderPlacedEvent event) {
        if (repository.existsByOrderIdAndType(event.orderId(), NotificationType.ORDER_PLACED)) {
            log.debug("ORDER_PLACED notification already exists for orderId={}, skip", event.orderId());
            return;
        }
        Notification notification = new Notification(
                event.orderId(),
                event.customerId(),
                NotificationType.ORDER_PLACED,
                "주문이 접수되었습니다. 주문번호: " + event.orderId()
        );
        deliver(notification, "ORDER_PLACED", event.orderId());
    }

    void on(OrderPaidEvent event) {
        if (repository.existsByOrderIdAndType(event.orderId(), NotificationType.ORDER_PAID)) {
            log.debug("ORDER_PAID notification already exists for orderId={}, skip", event.orderId());
            return;
        }
        Notification notification = new Notification(
                event.orderId(),
                event.customerId(),
                NotificationType.ORDER_PAID,
                "결제가 완료되었습니다. 주문번호: " + event.orderId()
        );
        deliver(notification, "ORDER_PAID", event.orderId());
    }

    private void deliver(Notification notification, String type, String orderId) {
        repository.save(notification);
        try {
            sender.send(notification);
            notification.markSent();
            repository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send {} notification for orderId={}", type, orderId, e);
            notification.markFailed();
            repository.save(notification);
        }
    }
}
