package com.minicommerce.inventory;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String productId) {
        super("Insufficient stock for product " + productId);
    }
}
