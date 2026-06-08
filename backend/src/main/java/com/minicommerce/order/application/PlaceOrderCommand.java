package com.minicommerce.order.application;

import java.util.List;

public record PlaceOrderCommand(List<OrderItem> items) {
    public record OrderItem(String productId, long quantity, String selectedOptionId) {}
}
