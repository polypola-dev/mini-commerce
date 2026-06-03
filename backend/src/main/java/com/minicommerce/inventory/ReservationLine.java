package com.minicommerce.inventory;

import jakarta.persistence.Embeddable;

@Embeddable
public class ReservationLine {
    private String productId;
    private long quantity;

    protected ReservationLine() {
    }

    public ReservationLine(String productId, long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public long getQuantity() {
        return quantity;
    }
}
