package com.minicommerce.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InventoryReservationTest {

    private static final UUID ORDER_1 = UUID.fromString("00000000-0000-7000-8000-0000000000e1");
    private static final UUID PROD_1 = UUID.fromString("00000000-0000-7000-8000-0000000000a1");

    private InventoryReservation reservation() {
        return new InventoryReservation(
                ORDER_1, ORDER_1, Instant.now().plusSeconds(600),
                List.of(new ReservationLine(PROD_1, 2L)));
    }

    @Test
    @DisplayName("restock: OVERSOLD 예약은 예외 없이 no-op(false) — 되돌려줄 재고가 없어 상태 유지")
    void restock_oversold_isNoOpWithoutException() {
        InventoryReservation reservation = reservation();
        reservation.release();
        reservation.markOversold();

        assertThatCode(() -> assertThat(reservation.restock()).isFalse())
                .doesNotThrowAnyException();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.OVERSOLD);
    }
}
