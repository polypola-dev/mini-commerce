package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.application.port.out.OrderNumberPort;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 표시 전용 주문번호 채번(GH #19)의 동시성·일별 리셋·KST 경계 안전망.
 *
 * <p>핵심은 "같은 날 다건 동시 생성 시 번호 중복/스킵이 없는지"다 — 카운터 행을 비관적 락으로
 * 잠그는 구현이 실제로 직렬화되는지 여러 스레드가 각자 트랜잭션에서 동시에 채번해 검증한다.
 * 커넥션 이중 점유(그날 첫 채번의 REQUIRES_NEW 경로)를 넉넉히 수용하도록 풀을 키워 둔다.
 */
@DataJpaTest
@Import({OrderNumberAdapter.class, OrderNumberSequenceInitializer.class})
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=40",
})
class OrderNumberAdapterTest {

    // 2026-07-22T15:30Z = KST 2026-07-23 00:30 → 채번 날짜는 07-23. UTC 자정 경계에 걸쳐도 KST 기준으로
    // 날짜가 결정되는지(자정 직후 주문이 그날로 묶이는지) 함께 검증한다.
    private static final Instant DAY_23 = Instant.parse("2026-07-22T15:30:00Z");
    private static final Instant DAY_24 = Instant.parse("2026-07-23T15:30:00Z");
    // 동시성 테스트는 별도 날짜(07-25)를 쓴다 — @DataJpaTest 롤백은 메인 스레드 tx만 되돌리므로
    // 워커가 커밋한 카운터 값이 DB에 남아 다른 테스트로 누수되는 것을 날짜 분리로 막는다.
    private static final Instant DAY_25 = Instant.parse("2026-07-24T15:30:00Z");

    @Autowired
    private OrderNumberPort orderNumberPort;

    @Autowired
    private PlatformTransactionManager txManager;

    private String generateInTx(Instant createdAt) {
        return new TransactionTemplate(txManager).execute(status -> orderNumberPort.generate(createdAt));
    }

    @Test
    @DisplayName("같은 날 다건 동시 채번: 번호가 0001부터 연속으로 중복/스킵 없이 발급된다")
    void concurrentGeneration_noDuplicateOrSkip() throws InterruptedException {
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<String> results = new CopyOnWriteArrayList<>();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    results.add(generateInTx(DAY_25));
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        go.countDown(); // 최대한 동시에 출발시켜 경합을 유도한다.
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(failures).hasValue(0);
        assertThat(results).hasSize(threads);
        // 중복 없음 + 1..threads 연속(스킵 없음).
        List<String> expected = IntStream.rangeClosed(1, threads)
                .mapToObj(n -> String.format("ORD-20260725-%04d", n))
                .collect(Collectors.toList());
        assertThat(results).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("일별 리셋: 날짜가 다르면 각자 0001부터 독립적으로 채번된다(KST 기준)")
    void differentDays_resetIndependently() {
        String first23 = generateInTx(DAY_23);
        String second23 = generateInTx(DAY_23);
        String first24 = generateInTx(DAY_24);

        assertThat(first23).isEqualTo("ORD-20260723-0001");
        assertThat(second23).isEqualTo("ORD-20260723-0002");
        // 다음 날은 다시 0001부터 — 누적 총량이 노출되지 않는다.
        assertThat(first24).isEqualTo("ORD-20260724-0001");
    }
}
