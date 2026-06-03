package com.minicommerce.inventory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, String> {
    List<InventoryReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);

    Optional<InventoryReservation> findByOrderId(String orderId);
}
