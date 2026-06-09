package com.minicommerce.notification;

import com.minicommerce.order.OrderPaidEvent;
import com.minicommerce.order.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationSender sender;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(repository, sender);
        // save()가 전달받은 Notification을 그대로 반환하도록 설정
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("OrderPlacedEvent 수신 시 SENT 상태로 알림이 2회 저장되고 sender가 1회 호출된다")
    void on_OrderPlacedEvent_savesTwiceAndSendsOnce() {
        // given
        OrderPlacedEvent event = new OrderPlacedEvent("order-1", "cust-1", BigDecimal.valueOf(10000));

        // when
        notificationService.on(event);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        verify(sender, times(1)).send(any(Notification.class));

        Notification saved = captor.getAllValues().get(1);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_PLACED);
    }

    @Test
    @DisplayName("OrderPaidEvent 수신 시 SENT 상태로 알림이 2회 저장되고 sender가 1회 호출된다")
    void on_OrderPaidEvent_savesTwiceAndSendsOnce() {
        // given
        OrderPaidEvent event = new OrderPaidEvent("order-1", "cust-1", BigDecimal.valueOf(10000));

        // when
        notificationService.on(event);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        verify(sender, times(1)).send(any(Notification.class));

        Notification saved = captor.getAllValues().get(1);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_PAID);
    }

    @Test
    @DisplayName("sender.send()에서 예외 발생 시 FAILED 상태로 저장된다")
    void on_OrderPlacedEvent_whenSenderThrows_savesAsFailed() {
        // given
        OrderPlacedEvent event = new OrderPlacedEvent("order-1", "cust-1", BigDecimal.valueOf(10000));
        doThrow(new RuntimeException("send error")).when(sender).send(any(Notification.class));

        // when
        notificationService.on(event);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());

        Notification saved = captor.getAllValues().get(1);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }
}
