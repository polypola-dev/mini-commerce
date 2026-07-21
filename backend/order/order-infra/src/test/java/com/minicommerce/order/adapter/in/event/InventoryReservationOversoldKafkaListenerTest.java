package com.minicommerce.order.adapter.in.event;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.minicommerce.inventory.InventoryReservationOversoldEvent;
import com.minicommerce.order.application.port.in.CancelOrderUseCase;
import com.minicommerce.order.domain.OrderStatus;
import com.minicommerce.order.domain.exception.OrderCancelNotAllowedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryReservationOversoldKafkaListenerTest {

    @Mock
    private CancelOrderUseCase cancelOrderUseCase;

    @InjectMocks
    private InventoryReservationOversoldKafkaListener listener;

    private Logger listenerLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        listenerLogger = (Logger) LoggerFactory.getLogger(InventoryReservationOversoldKafkaListener.class);
        appender = new ListAppender<>();
        appender.start();
        listenerLogger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        listenerLogger.detachAppender(appender);
    }

    private InventoryReservationOversoldEvent event() {
        return new InventoryReservationOversoldEvent("order-1", "order-1", Instant.now());
    }

    @Test
    @DisplayName("오버셀 이벤트 수신 → 주문 자동 취소+환불(cancelByAdmin) 호출")
    void onReservationOversold_cancelsOrder() {
        listener.onReservationOversold(event());

        verify(cancelOrderUseCase).cancelByAdmin("order-1", "재고 소진으로 결제 확정 실패 — 자동 취소/환불");
    }

    @Test
    @DisplayName("이미 CANCELED(재전달 멱등) → 예외를 삼키고 WARN으로 스킵(ERROR 아님)")
    void onReservationOversold_alreadyCanceled_isSkippedAtWarn() {
        doThrow(new OrderCancelNotAllowedException("order-1", OrderStatus.CANCELED))
                .when(cancelOrderUseCase).cancelByAdmin("order-1", "재고 소진으로 결제 확정 실패 — 자동 취소/환불");

        assertThatCode(() -> listener.onReservationOversold(event()))
                .doesNotThrowAnyException();

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    @DisplayName("CANCELED가 아닌 취소 불가 상태(SHIPPED — 환불 미실행 사고) → 예외를 삼키되 ERROR로 승격")
    void onReservationOversold_notCancelable_escalatesToError() {
        doThrow(new OrderCancelNotAllowedException("order-1", OrderStatus.SHIPPED))
                .when(cancelOrderUseCase).cancelByAdmin("order-1", "재고 소진으로 결제 확정 실패 — 자동 취소/환불");

        assertThatCode(() -> listener.onReservationOversold(event()))
                .doesNotThrowAnyException();

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
    }
}
