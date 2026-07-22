package com.minicommerce.inventory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.Meter;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    // id/orderId/productId가 uuid로 전환됐으므로(GH #20) 원장/조회 경계에는 유효 UUID를 쓴다.
    // 단 Redis 키(stock:*, reservation:*)와 availableStock/setStock의 productId 매개변수는 원래도 String이라
    // 그대로 문자열로 다룬다(서비스가 uuid.toString()으로 키를 만든다).
    private static final String ORDER_1 = "00000000-0000-7000-8000-0000000000e1";
    private static final UUID ORDER_1_UUID = UUID.fromString(ORDER_1);
    private static final String PROD_1 = "00000000-0000-7000-8000-0000000000a1";
    private static final UUID PROD_1_UUID = UUID.fromString(PROD_1);
    private static final String PROD_2 = "00000000-0000-7000-8000-0000000000a2";
    private static final UUID PROD_2_UUID = UUID.fromString(PROD_2);
    private static final String RES_1 = "00000000-0000-7000-8000-0000000000f1";
    private static final UUID RES_1_UUID = UUID.fromString(RES_1);
    private static final String MISSING = "00000000-0000-7000-8000-00000000ffff";
    private static final UUID MISSING_UUID = UUID.fromString(MISSING);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private OpenTelemetry openTelemetry;

    @Mock
    private Meter meter;

    @Mock
    private LongCounterBuilder counterBuilder;

    @Mock
    private LongCounter oversoldCounter;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        // opsForValue() 호출 시 mock ValueOperations 반환
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // OTel 계측 체인 스텁 — 생성자에서 오버셀 카운터를 빌드하므로 주입 전에 준비한다.
        when(openTelemetry.getMeter("inventory-api")).thenReturn(meter);
        when(meter.counterBuilder("inventory.reservation.oversold")).thenReturn(counterBuilder);
        when(counterBuilder.setDescription(anyString())).thenReturn(counterBuilder);
        when(counterBuilder.setUnit(anyString())).thenReturn(counterBuilder);
        when(counterBuilder.build()).thenReturn(oversoldCounter);
        inventoryService = new InventoryService(redisTemplate, reservationRepository, eventPublisher, openTelemetry);
    }

    // ----------------------------------------------------------------
    // availableStock (productId는 Redis 키 매개변수 — 원래도 String, uuid 파싱 대상 아님)
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
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.empty());
        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any(), any(), any());

        InventoryHold hold = inventoryService.reserveForOrder(ORDER_1, List.of(new InventoryItem(PROD_1, 3L)));

        // 예약 ID = orderId(멱등 키)
        assertThat(hold.reservationId()).isEqualTo(ORDER_1);
        assertThat(hold.items()).containsExactly(new InventoryItem(PROD_1, 3L));
        assertThat(hold.expiresAt()).isAfter(Instant.now());
        verify(reservationRepository).save(any(InventoryReservation.class));
    }

    @Test
    @DisplayName("reserveForOrder: Lua 0(품절) → 원장 RELEASED 전이 후 OutOfStockException")
    void reserveForOrder_outOfStock_marksLedgerReleased() {
        InventoryReservation saved = new InventoryReservation(
                ORDER_1_UUID, ORDER_1_UUID, Instant.now().plusSeconds(600),
                List.of(new ReservationLine(PROD_1_UUID, 99L)));
        when(reservationRepository.findByOrderId(ORDER_1_UUID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(saved)); // markReleased의 재조회
        when(reservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any(), any(), any());

        assertThatThrownBy(() -> inventoryService.reserveForOrder(ORDER_1,
                List.of(new InventoryItem(PROD_1, 99L))))
                .isInstanceOf(OutOfStockException.class)
                .hasMessageContaining(PROD_1);

        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("reserveForOrder: 재시도(기존 RESERVED 원장) → insert 없이 Lua 재실행(2=성공) 수렴")
    void reserveForOrder_retry_convergesWithoutDuplicateLedger() {
        InventoryReservation existing = new InventoryReservation(
                ORDER_1_UUID, ORDER_1_UUID, Instant.now().plusSeconds(600),
                List.of(new ReservationLine(PROD_1_UUID, 3L)));
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(existing));
        doReturn(2L).when(redisTemplate).execute(any(), anyList(), any(), any(), any());

        InventoryHold hold = inventoryService.reserveForOrder(ORDER_1, List.of(new InventoryItem(PROD_1, 3L)));

        assertThat(hold.reservationId()).isEqualTo(ORDER_1);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveForOrder: 원장이 RESERVED 외 상태(RELEASED) → ReservationConflictException, Lua 미실행")
    void reserveForOrder_conflictingLedger_throws() {
        InventoryReservation released = new InventoryReservation(
                ORDER_1_UUID, ORDER_1_UUID, Instant.now().minusSeconds(60),
                List.of(new ReservationLine(PROD_1_UUID, 3L)));
        released.release();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(released));

        assertThatThrownBy(() -> inventoryService.reserveForOrder(ORDER_1,
                List.of(new InventoryItem(PROD_1, 3L))))
                .isInstanceOf(ReservationConflictException.class);

        verify(redisTemplate, never()).execute(any(), anyList(), any(), any(), any());
    }

    // ----------------------------------------------------------------
    // releaseByOrderId
    // release Lua args: [qty] → 항목 1개 기준 varargs 1개
    // ----------------------------------------------------------------

    private InventoryReservation reservedReservation() {
        return new InventoryReservation(
                ORDER_1_UUID, ORDER_1_UUID, Instant.now().minusSeconds(60),
                List.of(new ReservationLine(PROD_1_UUID, 2L)));
    }

    @Test
    @DisplayName("releaseByOrderId: Lua 1(복원) → 원장 RELEASED 전이 + true")
    void releaseByOrderId_restored_transitionsLedger() {
        InventoryReservation reservation = reservedReservation();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any());

        assertThat(inventoryService.releaseByOrderId(ORDER_1)).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("releaseByOrderId: Lua 3(해시 없음 — 미차감 크래시 창) → INCRBY 없이 원장만 RELEASED + true")
    void releaseByOrderId_hashMissing_transitionsLedgerOnly() {
        InventoryReservation reservation = reservedReservation();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));
        doReturn(3L).when(redisTemplate).execute(any(), anyList(), any());

        assertThat(inventoryService.releaseByOrderId(ORDER_1)).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    @DisplayName("releaseByOrderId: Lua 0(해시가 CONFIRMED 등 — 결제가 이긴 경합) → 원장 유지 + false")
    void releaseByOrderId_paymentWon_keepsLedger() {
        InventoryReservation reservation = reservedReservation();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any());

        assertThat(inventoryService.releaseByOrderId(ORDER_1)).isFalse();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    @DisplayName("releaseByOrderId: 원장이 없거나 RESERVED가 아니면 no-op(false, 멱등)")
    void releaseByOrderId_missingOrInactive_isNoOp() {
        when(reservationRepository.findByOrderId(MISSING_UUID)).thenReturn(Optional.empty());
        assertThat(inventoryService.releaseByOrderId(MISSING)).isFalse();

        InventoryReservation confirmed = reservedReservation();
        confirmed.confirm();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(confirmed));
        assertThat(inventoryService.releaseByOrderId(ORDER_1)).isFalse();

        verify(redisTemplate, never()).execute(any(), anyList(), any());
    }

    // ----------------------------------------------------------------
    // confirmByOrderId
    // ----------------------------------------------------------------

    @Test
    @DisplayName("confirmByOrderId: RESERVED → 원장 CONFIRMED 전이 + Redis confirm Lua(재고 불변)")
    void confirmByOrderId_reserved_transitionsLedgerAndRedis() {
        InventoryReservation reservation = reservedReservation();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));

        inventoryService.confirmByOrderId(ORDER_1);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(redisTemplate).execute(any(), eq(List.of("reservation:" + ORDER_1)));
    }

    @Test
    @DisplayName("confirmByOrderId: 이미 CONFIRMED면 멱등 no-op(Redis 미접근)")
    void confirmByOrderId_alreadyConfirmed_isNoOp() {
        InventoryReservation reservation = reservedReservation();
        reservation.confirm();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));

        inventoryService.confirmByOrderId(ORDER_1);

        verify(redisTemplate, never()).execute(any(), anyList());
        verify(redisTemplate, never()).execute(any(), anyList(), any());
    }

    @Test
    @DisplayName("confirmByOrderId: RELEASED(리퍼가 이긴 경합) → force-confirm으로 재차감 + CONFIRMED 강제(payment-wins)")
    void confirmByOrderId_released_forceConfirms() {
        InventoryReservation reservation = reservedReservation();
        reservation.release(); // 리퍼가 먼저 해제
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any());

        inventoryService.confirmByOrderId(ORDER_1);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        // force-confirm Lua는 예약키 + stock 키를 함께 넘긴다(재차감)
        verify(redisTemplate).execute(any(), eq(List.of("reservation:" + ORDER_1, "stock:" + PROD_1)), eq("2"));
    }

    @Test
    @DisplayName("confirmByOrderId: 이미 RESTOCKED(취소·재입고된 주문)면 확정 미적용 + 경고(재전달 방어)")
    void confirmByOrderId_alreadyRestocked_skips() {
        InventoryReservation reservation = reservedReservation();
        reservation.confirm();
        reservation.restock();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));

        inventoryService.confirmByOrderId(ORDER_1);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESTOCKED);
        verify(redisTemplate, never()).execute(any(), anyList(), any());
    }

    @Test
    @DisplayName("confirmByOrderId: RELEASED인데 재고가 이미 채임(Lua 0) → OVERSOLD 표시 + 오버셀 이벤트 발행(자동 취소+환불 요청)")
    void confirmByOrderId_released_stockAlreadyClaimed_marksOversoldAndPublishes() {
        InventoryReservation reservation = reservedReservation();
        reservation.release(); // 리퍼가 먼저 해제
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any());

        inventoryService.confirmByOrderId(ORDER_1);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.OVERSOLD);
        ArgumentCaptor<InventoryReservationOversoldEvent> captor =
                ArgumentCaptor.forClass(InventoryReservationOversoldEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().reservationId()).isEqualTo(ORDER_1);
        assertThat(captor.getValue().orderId()).isEqualTo(ORDER_1);

        // 오버셀 카운터가 상품별 수량(prod-1, 2)으로 증가했는지 검증(product_id 태그 구분).
        ArgumentCaptor<Attributes> attrCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(oversoldCounter).add(eq(2L), attrCaptor.capture());
        assertThat(attrCaptor.getValue().get(AttributeKey.stringKey("product_id"))).isEqualTo(PROD_1);
    }

    @Test
    @DisplayName("confirmByOrderId: 이미 OVERSOLD(취소+환불 요청됨)면 멱등 스킵 + 이벤트 재발행 안 함(재전달 방어)")
    void confirmByOrderId_alreadyOversold_isNoOpWithoutRepublish() {
        InventoryReservation reservation = reservedReservation();
        reservation.release();
        reservation.markOversold();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));

        inventoryService.confirmByOrderId(ORDER_1);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.OVERSOLD);
        verify(redisTemplate, never()).execute(any(), anyList(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("confirmByOrderId: 원장이 없으면 EntityNotFoundException")
    void confirmByOrderId_missing_throws() {
        when(reservationRepository.findByOrderId(MISSING_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.confirmByOrderId(MISSING))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    // ----------------------------------------------------------------
    // restockByOrderId (GH #4)
    // ----------------------------------------------------------------

    private InventoryReservation confirmedReservation() {
        InventoryReservation reservation = new InventoryReservation(
                RES_1_UUID, ORDER_1_UUID, Instant.now().plusSeconds(600),
                List.of(new ReservationLine(PROD_1_UUID, 2L), new ReservationLine(PROD_2_UUID, 1L)));
        reservation.confirm();
        return reservation;
    }

    @Test
    @DisplayName("restockByOrderId: CONFIRMED 예약을 RESTOCKED로 전이하고 Lua로 재고를 복원한다")
    void restockByOrderId_confirmed_restoresStock() {
        InventoryReservation reservation = confirmedReservation();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));
        doReturn(1L).when(redisTemplate).execute(any(), anyList(), any(), any());

        inventoryService.restockByOrderId(ORDER_1);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESTOCKED);
        verify(redisTemplate).execute(any(), eq(List.of("reservation:" + RES_1, "stock:" + PROD_1, "stock:" + PROD_2)),
                eq("2"), eq("1"));
    }

    @Test
    @DisplayName("restockByOrderId: 이미 RESTOCKED면 Redis를 건드리지 않는다(멱등)")
    void restockByOrderId_alreadyRestocked_isNoOp() {
        InventoryReservation reservation = confirmedReservation();
        reservation.restock();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));

        inventoryService.restockByOrderId(ORDER_1);

        verify(redisTemplate, never()).execute(any(), anyList(), any(), any());
    }

    @Test
    @DisplayName("restockByOrderId: CONFIRMED가 아닌(RESERVED) 예약은 예외")
    void restockByOrderId_notConfirmed_throws() {
        InventoryReservation reservation = new InventoryReservation(
                RES_1_UUID, ORDER_1_UUID, Instant.now().plusSeconds(600),
                List.of(new ReservationLine(PROD_1_UUID, 2L)));
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> inventoryService.restockByOrderId(ORDER_1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("restockByOrderId: Lua 스크립트 결과 0(비정상) → 예외")
    void restockByOrderId_scriptRejects_throws() {
        InventoryReservation reservation = confirmedReservation();
        when(reservationRepository.findByOrderId(ORDER_1_UUID)).thenReturn(Optional.of(reservation));
        doReturn(0L).when(redisTemplate).execute(any(), anyList(), any(), any());

        assertThatThrownBy(() -> inventoryService.restockByOrderId(ORDER_1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("restockByOrderId: 예약 원장이 없으면 EntityNotFoundException")
    void restockByOrderId_missingReservation_throws() {
        when(reservationRepository.findByOrderId(MISSING_UUID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.restockByOrderId(MISSING))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }
}
