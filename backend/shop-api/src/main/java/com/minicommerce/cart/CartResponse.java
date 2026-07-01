package com.minicommerce.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String customerId,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {
    public static CartResponse from(Cart cart) {
        return new CartResponse(
                cart.getId(),
                cart.getItems().stream().map(CartItemResponse::from).toList(),
                cart.getTotalAmount()
        );
    }
}
