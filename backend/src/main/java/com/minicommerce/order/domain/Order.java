package com.minicommerce.order.domain;

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

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;

    private String customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;
    private Instant createdAt;

    private String shippingRecipient;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingDetailAddress;
    private String shippingZipCode;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(String id, String customerId, List<OrderLineDraft> lineDrafts) {
        this(id, customerId, lineDrafts, null, null, null, null, null);
    }

    public Order(String id, String customerId, List<OrderLineDraft> lineDrafts,
                 String shippingRecipient, String shippingPhone,
                 String shippingAddress, String shippingDetailAddress, String shippingZipCode) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
        this.totalAmount = lineDrafts.stream()
                .map(OrderLineDraft::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.lines = lineDrafts.stream()
                .map(draft -> new OrderLine(this, draft.productId(), draft.productName(), draft.unitPrice(), draft.quantity(), draft.selectedOptionValue()))
                .toList();
        this.shippingRecipient = shippingRecipient;
        this.shippingPhone = shippingPhone;
        this.shippingAddress = shippingAddress;
        this.shippingDetailAddress = shippingDetailAddress;
        this.shippingZipCode = shippingZipCode;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderLine> getLines() { return List.copyOf(lines); }
    public String getShippingRecipient() { return shippingRecipient; }
    public String getShippingPhone() { return shippingPhone; }
    public String getShippingAddress() { return shippingAddress; }
    public String getShippingDetailAddress() { return shippingDetailAddress; }
    public String getShippingZipCode() { return shippingZipCode; }

    public void markPaid() {
        if (status == OrderStatus.PENDING_PAYMENT) {
            status = OrderStatus.PAID;
        }
    }

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;
    }
}
