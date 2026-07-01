package com.minicommerce.inventory;

import java.time.Instant;
import java.util.List;

public record InventoryHold(String reservationId, Instant expiresAt, List<InventoryItem> items) {
}
