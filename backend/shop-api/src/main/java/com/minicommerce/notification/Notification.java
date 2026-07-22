package com.minicommerce.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import com.minicommerce.shared.UuidV7;

@Entity
@Table(name = "notifications")
class Notification {

    @Id
    private UUID id;

    private UUID orderId;

    private UUID customerId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(length = 500)
    private String message;

    private Instant createdAt;

    private Instant sentAt;

    protected Notification() {
    }

    Notification(UUID orderId, UUID customerId, NotificationType type, String message) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.type = type;
        this.status = NotificationStatus.PENDING;
        this.message = message;
    }

    @PrePersist
    void prePersist() {
        if (this.id == null) {
            this.id = UuidV7.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // 테스트에서 @PrePersist 없이 id/createdAt을 초기화할 때 사용
    void prePersistForTest() {
        prePersist();
    }

    void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = Instant.now();
    }

    void markFailed() {
        this.status = NotificationStatus.FAILED;
    }

    UUID getId() {
        return id;
    }

    UUID getOrderId() {
        return orderId;
    }

    UUID getCustomerId() {
        return customerId;
    }

    NotificationType getType() {
        return type;
    }

    NotificationStatus getStatus() {
        return status;
    }

    String getMessage() {
        return message;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Instant getSentAt() {
        return sentAt;
    }
}
