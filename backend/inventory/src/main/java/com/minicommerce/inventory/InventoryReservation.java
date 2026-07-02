package com.minicommerce.inventory;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {
    @Id
    private String id;

    private String orderId;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private Instant expiresAt;

    @ElementCollection
    private List<ReservationLine> lines = new ArrayList<>();

    protected InventoryReservation() {
    }

    public InventoryReservation(String id, String orderId, Instant expiresAt, List<ReservationLine> lines) {
        this.id = id;
        this.orderId = orderId;
        this.expiresAt = expiresAt;
        this.lines = new ArrayList<>(lines);
        this.status = ReservationStatus.RESERVED;
    }

    public String getId() {
        return id;
    }

    public String getOrderId() {
        return orderId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public List<ReservationLine> getLines() {
        return List.copyOf(lines);
    }

    public void confirm() {
        if (status == ReservationStatus.RESERVED) {
            status = ReservationStatus.CONFIRMED;
        }
    }

    public void release() {
        if (status == ReservationStatus.RESERVED) {
            status = ReservationStatus.RELEASED;
        }
    }
}
