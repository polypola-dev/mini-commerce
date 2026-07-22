package com.minicommerce.address.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "addresses")
public class AddressJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "label")
    private String label;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address1")
    private String address1;

    @Column(name = "address2")
    private String address2;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "created_at")
    private Instant createdAt;

    protected AddressJpaEntity() {
    }

    public AddressJpaEntity(UUID id, UUID customerId, String label, String recipientName, String phone,
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

    public UUID getId() {
        return id;
    }

    public UUID getCustomerId() {
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
