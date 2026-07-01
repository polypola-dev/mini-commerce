package com.minicommerce.cart;

public class CartFullException extends RuntimeException {
    public CartFullException(String customerId) {
        super("Cart is full (max 200 items) for customer " + customerId);
    }
}
