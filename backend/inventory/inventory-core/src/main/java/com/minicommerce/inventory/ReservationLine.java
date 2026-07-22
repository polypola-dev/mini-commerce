package com.minicommerce.inventory;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class ReservationLine {
    private UUID productId;
    private long quantity;

    protected ReservationLine() {
    }

    public ReservationLine(UUID productId, long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public UUID getProductId() {
        return productId;
    }

    public long getQuantity() {
        return quantity;
    }
}
