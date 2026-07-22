package com.minicommerce.notification;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<NotificationResponse> getMyNotifications(HttpServletRequest httpRequest) {
        String customerId = (String) httpRequest.getAttribute("authenticatedUserId");
        if (customerId == null) {
            return List.of();
        }
        return repository.findByCustomerIdOrderByCreatedAtDesc(UUID.fromString(customerId))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }
}
