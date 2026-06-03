package com.minicommerce.order;

import java.math.BigDecimal;

public record OrderLineDraft(
        String productId,
        String productName,
        BigDecimal unitPrice,
        long quantity
) {
    BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
