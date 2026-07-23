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

    // 이벤트 계약은 String이지만 NotificationService가 UUID.fromString으로 파싱하므로 유효 UUID를 쓴다(GH #20).
    private static final String ORDER_1 = "00000000-0000-7000-8000-0000000000e1";
    private static final String ORDER_NUMBER_1 = "ORD-20260101-0001";
    private static final String CUST_1 = "00000000-0000-7000-8000-0000000000c1";

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
        OrderPlacedEvent event = new OrderPlacedEvent(ORDER_1, ORDER_NUMBER_1, CUST_1, BigDecimal.valueOf(10000));

        // when
        notificationService.on(event);

        // then
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository, times(2)).save(captor.capture());
        verify(sender, times(1)).send(any(Notification.class));

        Notification saved = captor.getAllValues().get(1);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_PLACED);
        // 알림 메시지에는 내부 UUID가 아니라 표시 전용 주문번호가 담겨야 한다(GH #19).
        assertThat(saved.getMessage()).contains(ORDER_NUMBER_1).doesNotContain(ORDER_1);
    }

    @Test
    @DisplayName("OrderPaidEvent 수신 시 SENT 상태로 알림이 2회 저장되고 sender가 1회 호출된다")
    void on_OrderPaidEvent_savesTwiceAndSendsOnce() {
        // given
        OrderPaidEvent event = new OrderPaidEvent(ORDER_1, ORDER_NUMBER_1, CUST_1, BigDecimal.valueOf(10000));

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
        OrderPlacedEvent event = new OrderPlacedEvent(ORDER_1, ORDER_NUMBER_1, CUST_1, BigDecimal.valueOf(10000));
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
