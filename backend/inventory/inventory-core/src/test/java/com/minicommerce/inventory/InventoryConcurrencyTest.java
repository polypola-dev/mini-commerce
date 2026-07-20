package com.minicommerce.inventory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @BeforeAll
    static void setUp() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.afterPropertiesSet();

        // stubOnly: 호출 기록을 남기지 않아 100 스레드 동시 호출에서도 안전하다(검증은 Redis 값으로).
        InventoryReservationRepository repository = mock(
                InventoryReservationRepository.class, withSettings().stubOnly());
        when(repository.findByOrderId(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(InventoryReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService = new InventoryService(template, repository);
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
}
