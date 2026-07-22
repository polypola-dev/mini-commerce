package com.minicommerce.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, UUID> {
    List<InventoryReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);

    Optional<InventoryReservation> findByOrderId(UUID orderId);
}
