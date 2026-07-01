package com.minicommerce.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class LogNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationSender.class);

    @Override
    public void send(Notification notification) {
        log.info("Sending {} notification to customerId={} for orderId={}",
                notification.getType(),
                notification.getCustomerId(),
                notification.getOrderId());
    }
}
