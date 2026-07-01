package com.minicommerce.order.application;

import java.util.List;

public record PlaceOrderCommand(
        List<OrderItem> items,
        String shippingRecipient,
        String shippingPhone,
        String shippingAddress,
        String shippingDetailAddress,
        String shippingZipCode
) {
    public record OrderItem(String productId, long quantity, String selectedOptionId) {}
}
