package com.minicommerce.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationRepository repository;

    @BeforeEach
    void setUp() {
        // JWT 필터를 우회하기 위해 standaloneSetup 사용
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(repository))
                .build();
    }

    @Test
    @DisplayName("authenticatedUserId attribute가 없으면 빈 배열을 반환한다")
    void getMyNotifications_noAttribute_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("customerId로 알림 목록을 조회하면 200 OK와 알림 목록을 반환한다")
    void getMyNotifications_withCustomerId_returnsList() throws Exception {
        // given
        String customerId = "cust-1";
        Notification notification = new Notification("order-1", customerId, NotificationType.ORDER_PLACED,
                "주문이 접수되었습니다. 주문번호: order-1");
        notification.prePersistForTest();
        notification.markSent();

        when(repository.findByCustomerIdOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of(notification));

        // when & then
        mockMvc.perform(get("/api/notifications")
                        .requestAttr("authenticatedUserId", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-1"))
                .andExpect(jsonPath("$[0].customerId").value(customerId))
                .andExpect(jsonPath("$[0].type").value("ORDER_PLACED"))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }
}
