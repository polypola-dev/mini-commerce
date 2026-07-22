package com.minicommerce.address.domain;

import java.time.Instant;

/**
 * 사용자 배송지 주소록 항목 — 순수 도메인 POJO (기술 애너테이션 0).
 *
 * <p>소유자는 {@code customerId}(Supabase user id)이며, 한 사용자당 여러 배송지를 가질 수 있고
 * 그중 하나가 기본 배송지({@code defaultAddress})다. 기본 배송지 불변식(사용자당 최대 1개)은
 * 애플리케이션 서비스가 보장한다.
 */
public class Address {

    private final String id;
    private final String customerId;
    private String label;
    private String recipientName;
    private String phone;
    private String address1;
    private String address2;
    private String zipCode;
    private boolean defaultAddress;
    private final Instant createdAt;

    public Address(String id, String customerId, String label, String recipientName, String phone,
                   String address1, String address2, String zipCode, boolean defaultAddress, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.label = label;
        this.recipientName = recipientName;
        this.phone = phone;
        this.address1 = address1;
        this.address2 = address2;
        this.zipCode = zipCode;
        this.defaultAddress = defaultAddress;
        this.createdAt = createdAt;
    }

    /** 배송지 내용 수정. 소유자·기본배송지 여부·생성시각은 유지한다. */
    public void update(String label, String recipientName, String phone,
                       String address1, String address2, String zipCode) {
        this.label = label;
        this.recipientName = recipientName;
        this.phone = phone;
        this.address1 = address1;
        this.address2 = address2;
        this.zipCode = zipCode;
    }

    public void markDefault() {
        this.defaultAddress = true;
    }

    public void unmarkDefault() {
        this.defaultAddress = false;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getLabel() {
        return label;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getZipCode() {
        return zipCode;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
