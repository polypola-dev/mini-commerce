package com.minicommerce.address.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 배송지 등록 요청 본문. */
public record SaveAddressRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String phone,
        @NotBlank @Size(max = 255) String address1,
        @Size(max = 255) String address2
) {
}
