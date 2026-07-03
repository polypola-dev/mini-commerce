package com.minicommerce.inventory;

import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * order-batch 프로세스에서만 활성화한다({@code app.batch.enabled=true}, ADR-005 S4). order-api/
 * order-admin도 이 클래스를 컴포넌트스캔 범위에 두지만(inventory가 공통 의존이라), 명시적 플래그로
 * 게이팅해 "스케줄링 애너테이션 부재"에만 의존한 암묵적 격리보다 안전하게 이중화한다.
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
    @Transactional
    public void releaseExpiredReservations() {
        List<InventoryReservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.RESERVED,
                Instant.now()
        );

        for (InventoryReservation reservation : expired) {
            InventoryHold hold = new InventoryHold(
                    reservation.getId(),
                    reservation.getExpiresAt(),
                    reservation.getLines().stream()
                            .map(line -> new InventoryItem(line.getProductId(), line.getQuantity()))
                            .toList()
            );
            if (inventoryService.release(hold)) {
                reservation.release();
                eventPublisher.publishEvent(new ReservationExpiredEvent(reservation.getId(), reservation.getOrderId()));
            }
        }
    }
}
