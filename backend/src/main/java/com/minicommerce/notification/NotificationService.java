package com.minicommerce.notification;

import com.minicommerce.order.OrderPaidEvent;
import com.minicommerce.order.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationSender sender;

    NotificationService(NotificationRepository repository, NotificationSender sender) {
        this.repository = repository;
        this.sender = sender;
    }

    @ApplicationModuleListener
    void on(OrderPlacedEvent event) {
        Notification notification = new Notification(
                event.orderId(),
                event.customerId(),
                NotificationType.ORDER_PLACED,
                "주문이 접수되었습니다. 주문번호: " + event.orderId()
        );
        repository.save(notification);
        try {
            sender.send(notification);
            notification.markSent();
            repository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send ORDER_PLACED notification for orderId={}", event.orderId(), e);
            notification.markFailed();
            repository.save(notification);
        }
    }

    @ApplicationModuleListener
    void on(OrderPaidEvent event) {
        Notification notification = new Notification(
                event.orderId(),
                event.customerId(),
                NotificationType.ORDER_PAID,
                "결제가 완료되었습니다. 주문번호: " + event.orderId()
        );
        repository.save(notification);
        try {
            sender.send(notification);
            notification.markSent();
            repository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send ORDER_PAID notification for orderId={}", event.orderId(), e);
            notification.markFailed();
            repository.save(notification);
        }
    }
}
