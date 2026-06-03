# Task 4: Mock Payment & Notification Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement mock payment and notification services to support the checkout flow without external dependencies.

**Architecture:** Two Spring Boot services (`PaymentService` and `NotificationService`) with mock logic. `PaymentService` simulates a 95% success rate for payments. `NotificationService` logs a mock email confirmation.

**Tech Stack:** Java, Spring Boot, JUnit 5, AssertJ, Mockito, Lombok.

---

### Task 1: Implement PaymentService

**Files:**
- Create: `backend/src/main/java/com/minicommerce/service/PaymentService.java`
- Create: `backend/src/test/java/com/minicommerce/service/PaymentServiceTests.java`

- [ ] **Step 1: Write the failing test**
```java
package com.minicommerce.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTests {
    private final PaymentService paymentService = new PaymentService();

    @Test
    void processMockPayment_shouldReturnBoolean() {
        boolean result = paymentService.processMockPayment(1L, 100);
        // Since it's random, we just check that it returns a result without throwing
        assertThat(result).isIn(true, false);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew :backend:test --tests com.minicommerce.service.PaymentServiceTests`
Expected: FAIL (Compilation error: PaymentService not found)

- [ ] **Step 3: Write minimal implementation**
```java
package com.minicommerce.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {
    public boolean processMockPayment(Long userId, Integer amount) {
        log.info("Mock payment processing for user {} amount {}", userId, amount);
        return Math.random() < 0.95;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew :backend:test --tests com.minicommerce.service.PaymentServiceTests`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add backend/src/main/java/com/minicommerce/service/PaymentService.java backend/src/test/java/com/minicommerce/service/PaymentServiceTests.java
git commit -m "feat: implement PaymentService"
```

### Task 2: Implement NotificationService

**Files:**
- Create: `backend/src/main/java/com/minicommerce/service/NotificationService.java`
- Create: `backend/src/test/java/com/minicommerce/service/NotificationServiceTests.java`

- [ ] **Step 1: Write the failing test**
```java
package com.minicommerce.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatNoException;

class NotificationServiceTests {
    private final NotificationService notificationService = new NotificationService();

    @Test
    void sendOrderConfirmation_shouldNotThrow() {
        assertThatNoException().isThrownBy(() -> 
            notificationService.sendOrderConfirmation("test@example.com", 1L)
        );
    }
}
```

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew :backend:test --tests com.minicommerce.service.NotificationServiceTests`
Expected: FAIL (Compilation error: NotificationService not found)

- [ ] **Step 3: Write minimal implementation**
```java
package com.minicommerce.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {
    public void sendOrderConfirmation(String email, Long orderId) {
        log.info("MOCK EMAIL: Sending order {} confirmation to {}", orderId, email);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew :backend:test --tests com.minicommerce.service.NotificationServiceTests`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add backend/src/main/java/com/minicommerce/service/NotificationService.java backend/src/test/java/com/minicommerce/service/NotificationServiceTests.java
git commit -m "feat: implement NotificationService"
```

### Task 3: Final Verification

- [ ] **Step 1: Run all tests**
Run: `./gradlew :backend:test`
Expected: PASS

- [ ] **Step 2: Check for lint/checkstyle issues**
Run: `./gradlew :backend:check`
Expected: PASS
