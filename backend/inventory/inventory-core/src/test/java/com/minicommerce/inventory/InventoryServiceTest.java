package com.minicommerce.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        // opsForValue() 호출 시 mock ValueOperations 반환
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ----------------------------------------------------------------
    // availableStock
    // ----------------------------------------------------------------

    @Test
    @DisplayName("availableStock: Redis에 값이 있으면 Long 파싱 후 반환")
    void availableStock_whenKeyExists_returnsParsedValue() {
        when(valueOps.get("stock:prod-1")).thenReturn("50");

        long stock = inventoryService.availableStock("prod-1", 100L);

        assertThat(stock).isEqualTo(50L);
        verify(valueOps, never()).setIfAbsent(any(), any());
    }

    @Test
    @DisplayName("availableStock: Redis에 값이 없으면 기본값을 반환하되 Redis에 쓰지 않음(read-only)")
    void availableStock_whenKeyMissing_returnsDefaultWithoutWriting() {
        when(valueOps.get("stock:prod-1")).thenReturn(null);

        long stock = inventoryService.availableStock("prod-1", 100L);

        assertThat(stock).isEqualTo(100L);
        verify(valueOps, never()).setIfAbsent(any(), any());
        verify(valueOps, never()).set(any(), any());
    }

    // ----------------------------------------------------------------
    // reserveForOrder (GH #3 S3)
    // reserve Lua args: [qty, itemPayload, hashTtlSeconds] → 항목 1개 기준 varargs 3개
    // ----------------------------------------------------------------

    @Test
    @DisplayName("reserveForOrder: 신규 주문 → 원장 선기록(RESERVED) 후 Lua 1 → InventoryHold 반환")
    void reserveForOrder_new_persistsLedgerThenReserves() {
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.empty());
        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any(), any(), any());

        InventoryHold hold = inventoryService.reserveForOrder("order-1", List.of(new InventoryItem("prod-1", 3L)));

        // 예약 ID = orderId(멱등 키)
        assertThat(hold.reservationId()).isEqualTo("order-1");
        assertThat(hold.items()).containsExactly(new InventoryItem("prod-1", 3L));
        assertThat(hold.expiresAt()).isAfter(Instant.now());
        verify(reservationRepository).save(any(InventoryReservation.class));
    }

    @Test
    @DisplayName("reserveForOrder: Lua 0(품절) → 원장 RELEASED 전이 후 OutOfStockException")
    void reserveForOrder_outOfStock_marksLedgerReleased() {
        InventoryReservation saved = new InventoryReservation(
                "order-1", "order-1", Instant.now().plusSeconds(600),
                List.of(new ReservationLine("prod-1", 99L)));
        when(reservationRepository.findByOrderId("order-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(saved)); // markReleased의 재조회
        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any(), any(), any());

        assertThatThrownBy(() -> inventoryService.reserveForOrder("order-1",
                List.of(new InventoryItem("prod-1", 99L))))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining("prod-1");

        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("reserveForOrder: 재시도(기존 RESERVED 원장) → insert 없이 Lua 재실행(2=성공) 수렴")
    void reserveForOrder_retry_convergesWithoutDuplicateLedger() {
        InventoryReservation existing = new InventoryReservation(
                "order-1", "order-1", Instant.now().plusSeconds(600),
                List.of(new ReservationLine("prod-1", 3L)));
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(existing));
        doReturn(2L).when(redisTemplate).execute(any(), anyList(), any(), any(), any());

        InventoryHold hold = inventoryService.reserveForOrder("order-1", List.of(new InventoryItem("prod-1", 3L)));

        assertThat(hold.reservationId()).isEqualTo("order-1");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveForOrder: 원장이 RESERVED 외 상태(RELEASED) → ReservationConflictException, Lua 미실행")
    void reserveForOrder_conflictingLedger_throws() {
        InventoryReservation released = new InventoryReservation(
                "order-1", "order-1", Instant.now().minusSeconds(60),
                List.of(new ReservationLine("prod-1", 3L)));
        released.release();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(released));

        assertThatThrownBy(() -> inventoryService.reserveForOrder("order-1",
                List.of(new InventoryItem("prod-1", 3L))))
                .isInstanceOf(ReservationConflictException.class);

        verify(redisTemplate, never()).execute(any(), anyList(), any(), any(), any());
    }

    // ----------------------------------------------------------------
    // releaseByOrderId
    // release Lua args: [qty] → 항목 1개 기준 varargs 1개
    // ----------------------------------------------------------------

    private InventoryReservation reservedReservation() {
        return new InventoryReservation(
                "order-1", "order-1", Instant.now().minusSeconds(60),
                List.of(new ReservationLine("prod-1", 2L)));
    }

    @Test
    @DisplayName("releaseByOrderId: Lua 1(복원) → 원장 RELEASED 전이 + true")
    void releaseByOrderId_restored_transitionsLedger() {
        InventoryReservation reservation = reservedReservation();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any());

        assertThat(inventoryService.releaseByOrderId("order-1")).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("releaseByOrderId: Lua 3(해시 없음 — 미차감 크래시 창) → INCRBY 없이 원장만 RELEASED + true")
    void releaseByOrderId_hashMissing_transitionsLedgerOnly() {
        InventoryReservation reservation = reservedReservation();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));
        doReturn(3L).when(redisTemplate).execute(any(), anyList(), any());

        assertThat(inventoryService.releaseByOrderId("order-1")).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("releaseByOrderId: Lua 0(해시가 CONFIRMED 등 — 결제가 이긴 경합) → 원장 유지 + false")
    void releaseByOrderId_paymentWon_keepsLedger() {
        InventoryReservation reservation = reservedReservation();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any());

        assertThat(inventoryService.releaseByOrderId("order-1")).isFalse();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("releaseByOrderId: 원장이 없거나 RESERVED가 아니면 no-op(false, 멱등)")
    void releaseByOrderId_missingOrInactive_isNoOp() {
        when(reservationRepository.findByOrderId("missing")).thenReturn(Optional.empty());
        assertThat(inventoryService.releaseByOrderId("missing")).isFalse();

        InventoryReservation confirmed = reservedReservation();
        confirmed.confirm();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(confirmed));
        assertThat(inventoryService.releaseByOrderId("order-1")).isFalse();

        verify(redisTemplate, never()).execute(any(), anyList(), any());
    }

    // ----------------------------------------------------------------
    // confirmByOrderId
    // ----------------------------------------------------------------

    @Test
    @DisplayName("confirmByOrderId: RESERVED → 원장 CONFIRMED 전이 + Redis confirm Lua(재고 불변)")
    void confirmByOrderId_reserved_transitionsLedgerAndRedis() {
        InventoryReservation reservation = reservedReservation();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        inventoryService.confirmByOrderId("order-1");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(redisTemplate).execute(any(), eq(List.of("reservation:order-1")));
    }

    @Test
    @DisplayName("confirmByOrderId: 이미 CONFIRMED면 멱등 no-op(Redis 미접근)")
    void confirmByOrderId_alreadyConfirmed_isNoOp() {
        InventoryReservation reservation = reservedReservation();
        reservation.confirm();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        inventoryService.confirmByOrderId("order-1");

        verify(redisTemplate, never()).execute(any(), anyList());
        verify(redisTemplate, never()).execute(any(), anyList(), any());
    }

    @Test
    @DisplayName("confirmByOrderId: RELEASED(리퍼가 이긴 경합) → force-confirm으로 재차감 + CONFIRMED 강제(payment-wins)")
    void confirmByOrderId_released_forceConfirms() {
        InventoryReservation reservation = reservedReservation();
        reservation.release(); // 리퍼가 먼저 해제
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any());

        inventoryService.confirmByOrderId("order-1");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        // force-confirm Lua는 예약키 + stock 키를 함께 넘긴다(재차감)
        verify(redisTemplate).execute(any(), eq(List.of("reservation:order-1", "stock:prod-1")), eq("2"));
    }

    @Test
    @DisplayName("confirmByOrderId: 이미 RESTOCKED(취소·재입고된 주문)면 확정 미적용 + 경고(재전달 방어)")
    void confirmByOrderId_alreadyRestocked_skips() {
        InventoryReservation reservation = reservedReservation();
        reservation.confirm();
        reservation.restock();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        inventoryService.confirmByOrderId("order-1");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESTOCKED);
        verify(redisTemplate, never()).execute(any(), anyList(), any());
    }

    @Test
    @DisplayName("confirmByOrderId: 원장이 없으면 EntityNotFoundException")
    void confirmByOrderId_missing_throws() {
        when(reservationRepository.findByOrderId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.confirmByOrderId("missing"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    // ----------------------------------------------------------------
    // restockByOrderId (GH #4)
    // ----------------------------------------------------------------

    private InventoryReservation confirmedReservation() {
        InventoryReservation reservation = new InventoryReservation(
                "res-1", "order-1", Instant.now().plusSeconds(600),
                List.of(new ReservationLine("prod-1", 2L), new ReservationLine("prod-2", 1L)));
        reservation.confirm();
        return reservation;
    }

    @Test
    @DisplayName("restockByOrderId: CONFIRMED 예약을 RESTOCKED로 전이하고 Lua로 재고를 복원한다")
    void restockByOrderId_confirmed_restoresStock() {
        InventoryReservation reservation = confirmedReservation();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any(), any());

        inventoryService.restockByOrderId("order-1");

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESTOCKED);
        verify(redisTemplate).execute(any(), eq(List.of("reservation:res-1", "stock:prod-1", "stock:prod-2")),
                eq("2"), eq("1"));
    }

    @Test
    @DisplayName("restockByOrderId: 이미 RESTOCKED면 Redis를 건드리지 않는다(멱등)")
    void restockByOrderId_alreadyRestocked_isNoOp() {
        InventoryReservation reservation = confirmedReservation();
        reservation.restock();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        inventoryService.restockByOrderId("order-1");

        verify(redisTemplate, never()).execute(any(), anyList(), any(), any());
    }

    @Test
    @DisplayName("restockByOrderId: CONFIRMED가 아닌(RESERVED) 예약은 예외")
    void restockByOrderId_notConfirmed_throws() {
        InventoryReservation reservation = new InventoryReservation(
                "res-1", "order-1", Instant.now().plusSeconds(600),
                List.of(new ReservationLine("prod-1", 2L)));
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> inventoryService.restockByOrderId("order-1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("restockByOrderId: Lua 스크립트 결과 0(비정상) → 예외")
    void restockByOrderId_scriptRejects_throws() {
        InventoryReservation reservation = confirmedReservation();
        when(reservationRepository.findByOrderId("order-1")).thenReturn(Optional.of(reservation));
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any(), any());

        assertThatThrownBy(() -> inventoryService.restockByOrderId("order-1"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("restockByOrderId: 예약 원장이 없으면 EntityNotFoundException")
    void restockByOrderId_missingReservation_throws() {
        when(reservationRepository.findByOrderId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.restockByOrderId("missing"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}
