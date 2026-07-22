package com.minicommerce.address.adapter.in.web;

import com.minicommerce.address.domain.Address;

/**
 * 배송지 응답 DTO. 프론트 {@code lib/addresses.ts}의 {@code Address} 타입과 필드가 대응한다
 * ({@code name}=수령인, {@code isDefault}=기본 배송지 여부).
 */
public record AddressResponse(
        String id,
        String name,
        String phone,
        String address1,
        String address2,
        boolean isDefault
) {
    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getPhone(),
                address.getAddress1(),
                address.getAddress2(),
                address.isDefaultAddress()
        );
    }
}
