package com.minicommerce.inventory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Redis Lua Script 원자성 기반 동시성 검증 통합 테스트.
 * docker-compose로 실행 중인 로컬 Redis(localhost:6379)를 사용합니다.
 * DB 원장은 관심사가 아니므로(Lua의 재고 차감 원자성만 검증) 스레드 안전한 stub 리포지토리를 쓴다.
 */
class InventoryConcurrencyTest {

    private static InventoryService inventoryService;
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate template;

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();

        // stubOnly: 호출 기록을 남기지 않아 100 스레드 동시 호출에서도 안전하다(검증은 Redis 값으로).
        InventoryReservationRepository repository = mock(
                InventoryReservationRepository.class, withSettings().stubOnly());
        when(repository.findByOrderId(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 메트릭 검증이 아닌 Redis 경합 검증이 목적이므로 OTel은 no-op으로 둔다.
        inventoryService = new InventoryService(
                template, repository, mock(org.springframework.context.ApplicationEventPublisher.class),
                io.opentelemetry.api.OpenTelemetry.noop());
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("재고 10개에 100개 동시 예약(주문별 orderId) → 정확히 10개만 성공, 재고 0 잔여")
    void concurrentReserve_exactlyStockSucceeds() throws InterruptedException {
        String productId = "concurrent-prod-" + UUID.randomUUID();
        inventoryService.setStock(productId, 10L);

        int requests = 100;
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requests);

        for (int i = 0; i < requests; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    inventoryService.reserveForOrder(
                            UUID.randomUUID().toString(),
                            List.of(new InventoryItem(productId, 1L)));
                    success.incrementAndGet();
                } catch (OutOfStockException e) {
                    fail.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();

        assertThat(success.get()).isEqualTo(10);
        assertThat(fail.get()).isEqualTo(90);
        assertThat(inventoryService.availableStock(productId, 0)).isZero();
    }

    // ----------------------------------------------------------------
    // force-confirm(payment-wins) 실 Redis 시나리오
    // reserve→release→(다른 주문 채감)→confirmByOrderId가 실제 Lua 원장에서 오버셀을 막는지 검증.
    // confirmByOrderId는 예약 원장을 조회/상태전이하므로 상태를 보존하는 인메모리 리포지토리가 필요하다
    // (기존 setUp의 stubOnly 목은 항상 empty를 반환해 이 시퀀스에 쓸 수 없다).
    // ----------------------------------------------------------------

    private static InventoryService serviceWithStatefulLedger() {
        Map<String, InventoryReservation> store = new ConcurrentHashMap<>();
        InventoryReservationRepository repository = mock(InventoryReservationRepository.class);
        when(repository.findByOrderId(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.<String>getArgument(0))));
        when(repository.save(any(InventoryReservation.class))).thenAnswer(invocation -> {
            InventoryReservation reservation = invocation.getArgument(0);
            store.put(reservation.getOrderId(), reservation);
            return reservation;
        });
        return new InventoryService(
                template, repository, mock(org.springframework.context.ApplicationEventPublisher.class),
                io.opentelemetry.api.OpenTelemetry.noop());
    }

    @Test
    @DisplayName("force-confirm 오버셀 방지: 리퍼 release 후 다른 주문이 재고를 채가면 원 주문은 OVERSOLD로 귀결, 재고 음수 안 됨")
    void forceConfirm_stockClaimedByAnother_marksOversoldWithoutNegativeStock() {
        InventoryService service = serviceWithStatefulLedger();
        String productId = "oversold-prod-" + UUID.randomUUID();
        service.setStock(productId, 1L);

        String orderA = UUID.randomUUID().toString();
        String orderB = UUID.randomUUID().toString();

        // 주문 A 예약 → 재고 1→0
        service.reserveForOrder(orderA, List.of(new InventoryItem(productId, 1L)));
        assertThat(service.availableStock(productId, -1)).isZero();

        // 리퍼처럼 A 예약 해제 → 재고 0→1, A 원장 RELEASED
        assertThat(service.releaseByOrderId(orderA)).isTrue();
        assertThat(service.availableStock(productId, -1)).isEqualTo(1L);
        assertThat(service.getByOrderId(orderA).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.RELEASED);

        // 다른 주문 B가 즉시 재고를 채감 → 재고 1→0
        service.reserveForOrder(orderB, List.of(new InventoryItem(productId, 1L)));
        assertThat(service.availableStock(productId, -1)).isZero();

        // A에 대해 결제 확정(force-confirm 경로) → 재고 없음 → OVERSOLD, DECRBY 미실행
        service.confirmByOrderId(orderA);

        assertThat(service.getByOrderId(orderA).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.OVERSOLD);
        // 오버셀을 정직하게 처리하고 재고를 음수로 내리지 않았다(정확히 0 유지 — B가 가진 1건 차감 그대로).
        assertThat(service.availableStock(productId, -1)).isZero();
    }

    @Test
    @DisplayName("force-confirm 정상: 아무도 안 채간 재고를 정확히 1회만 재차감해 CONFIRMED, 재시도해도 중복 차감 없음")
    void forceConfirm_stockStillAvailable_confirmsExactlyOnce() {
        InventoryService service = serviceWithStatefulLedger();
        String productId = "forceconfirm-prod-" + UUID.randomUUID();
        service.setStock(productId, 1L);

        String orderA = UUID.randomUUID().toString();

        // 주문 A 예약 → 재고 1→0
        service.reserveForOrder(orderA, List.of(new InventoryItem(productId, 1L)));
        // 리퍼처럼 A 예약 해제 → 재고 0→1, RELEASED
        assertThat(service.releaseByOrderId(orderA)).isTrue();
        assertThat(service.availableStock(productId, -1)).isEqualTo(1L);

        // 아무도 채가지 않은 상태에서 A 결제 확정(force-confirm) → 정확히 1회 재차감 → 재고 0, CONFIRMED
        service.confirmByOrderId(orderA);
        assertThat(service.getByOrderId(orderA).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(service.availableStock(productId, -1)).isZero();

        // 재전달처럼 동일 확정을 재시도해도 추가 차감이 없어야 한다(멱등 — 재고 0 유지, 음수 아님).
        service.confirmByOrderId(orderA);
        assertThat(service.getByOrderId(orderA).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(service.availableStock(productId, -1)).isZero();
    }
}
