package com.minicommerce.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class InventoryReservationTest {

    private InventoryReservation reservation() {
        return new InventoryReservation(
                "order-1", "order-1", Instant.now().plusSeconds(600),
                List.of(new ReservationLine("prod-1", 2L)));
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
