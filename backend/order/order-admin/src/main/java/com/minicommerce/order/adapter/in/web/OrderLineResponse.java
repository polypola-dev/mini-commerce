package com.minicommerce.order.adapter.in.web;

import com.minicommerce.order.domain.OrderLine;
import java.math.BigDecimal;

public record OrderLineResponse(
        String productId,
        String productName,
        BigDecimal unitPrice,
        long quantity,
        BigDecimal subtotal,
        String selectedOptionValue
) {
    public static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(
                line.getProductId(),
                line.getProductName(),
                line.getUnitPrice(),
                line.getQuantity(),
                line.getSubtotal(),
                line.getSelectedOptionValue()
        );
    }
}
