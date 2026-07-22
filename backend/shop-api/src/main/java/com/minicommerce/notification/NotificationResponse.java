package com.minicommerce.notification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String orderId,
        String customerId,
        String type,
        String status,
        String message,
        Instant createdAt,
        Instant sentAt
) {
    static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId() != null ? n.getId().toString() : null,
                n.getOrderId() != null ? n.getOrderId().toString() : null,
                n.getCustomerId() != null ? n.getCustomerId().toString() : null,
                n.getType().name(),
                n.getStatus().name(),
                n.getMessage(),
                n.getCreatedAt(),
                n.getSentAt()
        );
    }
}
