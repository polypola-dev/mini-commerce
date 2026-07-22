package com.minicommerce.address.domain;

/** 소유자에게 존재하지 않는 배송지를 조회/변경할 때 던지는 도메인 예외. */
public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String addressId) {
        super("Address not found: " + addressId);
    }
}
