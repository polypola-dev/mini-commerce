package com.minicommerce.order.adapter.out.persistence;

import com.minicommerce.order.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 주문의 JPA 영속성 모델. 도메인 {@code Order}와 분리되어 기술 매핑만 담당한다. */
@Entity
@Table(name = "orders")
class OrderJpaEntity {
    @Id
    private UUID id;

    private UUID customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;
    private Instant createdAt;
    private String paymentKey;

    private String shippingRecipient;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingDetailAddress;
    private String shippingZipCode;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLineJpaEntity> lines = new ArrayList<>();

    protected OrderJpaEntity() {
    }

    OrderJpaEntity(UUID id, UUID customerId, OrderStatus status, BigDecimal totalAmount, Instant createdAt,
                   String paymentKey, String shippingRecipient, String shippingPhone, String shippingAddress,
                   String shippingDetailAddress, String shippingZipCode) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.paymentKey = paymentKey;
        this.shippingRecipient = shippingRecipient;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.shippingDetailAddress = shippingDetailAddress;
        this.shippingZipCode = shippingZipCode;
    }

    void addLine(OrderLineJpaEntity line) {
        line.assignOrder(this);
        this.lines.add(line);
    }

    UUID getId() { return id; }
    UUID getCustomerId() { return customerId; }
    OrderStatus getStatus() { return status; }
    BigDecimal getTotalAmount() { return totalAmount; }
    Instant getCreatedAt() { return createdAt; }
    String getPaymentKey() { return paymentKey; }
    List<OrderLineJpaEntity> getLines() { return lines; }
    String getShippingRecipient() { return shippingRecipient; }
    String getShippingPhone() { return shippingPhone; }
    String getShippingAddress() { return shippingAddress; }
    String getShippingDetailAddress() { return shippingDetailAddress; }
    String getShippingZipCode() { return shippingZipCode; }
}
