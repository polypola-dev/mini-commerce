package com.minicommerce.order.domain;

import java.math.BigDecimal;

public record OrderLineDraft(
        String productId,
        String productName,
        BigDecimal unitPrice,
        long quantity,
        String selectedOptionValue
) {
    BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
