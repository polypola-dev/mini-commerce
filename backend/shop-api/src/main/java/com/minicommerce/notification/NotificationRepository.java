package com.minicommerce.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
