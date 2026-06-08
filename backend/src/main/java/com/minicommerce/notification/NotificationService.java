package com.minicommerce.notification;

import com.minicommerce.order.domain.OrderPaidEvent;
import com.minicommerce.order.domain.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Service
class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @ApplicationModuleListener
    void on(OrderPlacedEvent event) {
        log.info("[NOTIFICATION] 주문 접수 이메일 발송 → customerId={} orderId={} amount={}",
                event.customerId(), event.orderId(), event.totalAmount());
    }

    @ApplicationModuleListener
    void on(OrderPaidEvent event) {
        log.info("[NOTIFICATION] 결제 완료 이메일 발송 → customerId={} orderId={} amount={}",
                event.customerId(), event.orderId(), event.totalAmount());
    }
}
