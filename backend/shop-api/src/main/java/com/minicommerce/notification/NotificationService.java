package com.minicommerce.notification;

import com.minicommerce.order.OrderCanceledEvent;
import com.minicommerce.order.OrderPaidEvent;
import com.minicommerce.order.OrderPlacedEvent;
import java.util.UUID;
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
        // orderId/customerId는 이벤트 계약상 String(order-events 무변경). DB/엔티티는 uuid이므로 여기서 변환.
        UUID orderUuid = UUID.fromString(event.orderId());
        if (repository.existsByOrderIdAndType(orderUuid, NotificationType.ORDER_PLACED)) {
            log.debug("ORDER_PLACED notification already exists for orderId={}, skip", event.orderId());
            return;
        }
        Notification notification = new Notification(
                orderUuid,
                UUID.fromString(event.customerId()),
                NotificationType.ORDER_PLACED,
                "주문이 접수되었습니다. 주문번호: " + displayNumber(event.orderNumber(), event.orderId())
        );
        deliver(notification, "ORDER_PLACED", event.orderId());
    }

    void on(OrderPaidEvent event) {
        UUID orderUuid = UUID.fromString(event.orderId());
        if (repository.existsByOrderIdAndType(orderUuid, NotificationType.ORDER_PAID)) {
            log.debug("ORDER_PAID notification already exists for orderId={}, skip", event.orderId());
            return;
        }
        Notification notification = new Notification(
                orderUuid,
                UUID.fromString(event.customerId()),
                NotificationType.ORDER_PAID,
                "결제가 완료되었습니다. 주문번호: " + displayNumber(event.orderNumber(), event.orderId())
        );
        deliver(notification, "ORDER_PAID", event.orderId());
    }

    void on(OrderCanceledEvent event) {
        UUID orderUuid = UUID.fromString(event.orderId());
        if (repository.existsByOrderIdAndType(orderUuid, NotificationType.ORDER_CANCELED)) {
            log.debug("ORDER_CANCELED notification already exists for orderId={}, skip", event.orderId());
            return;
        }
        Notification notification = new Notification(
                orderUuid,
                UUID.fromString(event.customerId()),
                NotificationType.ORDER_CANCELED,
                "주문이 취소되었습니다. 주문번호: " + displayNumber(event.orderNumber(), event.orderId())
        );
        deliver(notification, "ORDER_CANCELED", event.orderId());
    }

    /** 표시 전용 주문번호가 있으면 그것을, 없으면(과거 이벤트/미채번) 내부 orderId로 폴백한다. */
    private static String displayNumber(String orderNumber, String orderId) {
        return (orderNumber != null && !orderNumber.isBlank()) ? orderNumber : orderId;
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
