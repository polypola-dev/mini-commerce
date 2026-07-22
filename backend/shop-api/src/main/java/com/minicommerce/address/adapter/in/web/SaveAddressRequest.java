package com.minicommerce.address.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 배송지 등록/수정 요청 본문. 실무 수준 필드 검증(경계에서 강제):
 * <ul>
 *   <li>{@code label} 배송지명 — 최대 10자, 선택</li>
 *   <li>{@code name} 수령인 — 2~30자, 한글/영문/공백만</li>
 *   <li>{@code phone} 연락처 — 휴대폰 형식(하이픈 유무 허용)</li>
 *   <li>{@code address1} 도로명/지번 주소 — 우편번호 검색으로 채운 값</li>
 *   <li>{@code address2} 상세주소 — 최대 100자</li>
 *   <li>{@code zipCode} 우편번호 — 5자리 숫자(신우편번호), 선택</li>
 * </ul>
 */
public record SaveAddressRequest(
        @Size(max = 10) String label,
        @NotBlank @Size(min = 2, max = 30)
        @Pattern(regexp = "^[가-힣a-zA-Z ]+$", message = "수령인은 한글/영문만 입력할 수 있습니다.") String name,
        @NotBlank
        @Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$", message = "연락처 형식이 올바르지 않습니다.") String phone,
        @NotBlank @Size(max = 255) String address1,
        @Size(max = 100) String address2,
        @Pattern(regexp = "^$|^\\d{5}$", message = "우편번호는 5자리 숫자여야 합니다.") String zipCode
) {
}
