package com.minicommerce.inventory;

import java.time.Instant;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 만료된 재고 예약(RESERVED + expires_at 경과)을 해제하는 리퍼 — TTL 사가의 백스톱.
 *
 * <p>inventory 완전분리(GH #3 S3) 후에는 inventory-api가 이 빈을 소유한다
 * ({@code app.batch.enabled=true} — 과거 order-batch에서 이관, LockProvider도 inventory-api의
 * SchedulerLockConfig가 소유). 해제 성공 시 {@link InventoryReservationExpiredEvent}를 발행하고,
 * inventory-api의 Modulith 아웃박스가 Kafka {@code inventory.reservation.expired}로 외부화해
 * order-batch가 주문을 EXPIRED로 전이한다.
 *
 * <p>release Lua가 "해시 없음"(reserve의 원장 커밋↔Lua 사이 크래시 창)을 return 3으로 구분해
 * INCRBY 없이 원장만 RELEASED로 전이한다 — 재고 누수와 원장 고착을 모두 방지(InventoryService 참고).
 */
@Component
@ConditionalOnProperty(name = "app.batch.enabled", havingValue = "true")
public class ExpiredReservationReleaser {
    private final InventoryReservationRepository reservationRepository;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher eventPublisher;

    public ExpiredReservationReleaser(
            InventoryReservationRepository reservationRepository,
            InventoryService inventoryService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "expiredReservationReleaser", lockAtLeastFor = "PT10S", lockAtMostFor = "PT2M")
    @Transactional
    public void releaseExpiredReservations() {
        List<InventoryReservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED,
                Instant.now()
        );

        for (InventoryReservation reservation : expired) {
            // 서비스/이벤트 계약은 String(REST·Kafka 경계 무변경) — 엔티티의 uuid를 toString으로 맞춘다.
            if (inventoryService.releaseByOrderId(reservation.getOrderId().toString())) {
                eventPublisher.publishEvent(new InventoryReservationExpiredEvent(
                        reservation.getId().toString(), reservation.getOrderId().toString(), Instant.now()));
            }
        }
    }
}
