package com.minicommerce.inventory;

import java.time.Instant;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExpiredReservationReleaser {
    private final InventoryReservationRepository reservationRepository;
    private final InventoryService inventoryService;

    public ExpiredReservationReleaser(
            InventoryReservationRepository reservationRepository,
            InventoryService inventoryService
    ) {
        this.reservationRepository = reservationRepository;
        this.inventoryService = inventoryService;
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
            }
        }
    }
}
