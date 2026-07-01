package com.minicommerce.inventory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Lua Script 원자성 기반 동시성 검증 통합 테스트.
 * docker-compose로 실행 중인 로컬 Redis(localhost:6379)를 사용합니다.
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
        inventoryService = new InventoryService(template);
    }

    @AfterAll
    static void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    @DisplayName("재고 10개에 100개 동시 요청 → 정확히 10개만 성공, 재고 0 잔여")
    void concurrentReserve_exactlyStockSucceeds() throws InterruptedException {
        String productId = "concurrent-prod-" + UUID.randomUUID();
        inventoryService.initializeStockIfAbsent(productId, 10L);

        int requests = 100;
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requests);

        for (int i = 0; i < requests; i++) {
            new Thread(() -> {
                try {
                    start.await();
                    inventoryService.reserve(List.of(new InventoryItem(productId, 1L)));
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
