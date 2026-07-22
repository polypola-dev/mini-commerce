package com.minicommerce.cart;

import java.math.BigDecimal;

public record CartItemResponse(
        String itemId,
        String productId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        String selectedOptionId,
        String selectedOptionValue
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId().toString(),
                item.getProductId().toString(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getSelectedOptionId() != null ? item.getSelectedOptionId().toString() : null,
                item.getSelectedOptionValue()
        );
    }
}
