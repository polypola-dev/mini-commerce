package com.minicommerce.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    // Kafka at-least-once 대비 멱등 가드: (orderId, type) 조합당 알림 1건
    boolean existsByOrderIdAndType(UUID orderId, NotificationType type);
}
