package com.minicommerce.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    // Kafka at-least-once 대비 멱등 가드: (orderId, type) 조합당 알림 1건
    boolean existsByOrderIdAndType(String orderId, NotificationType type);
}
