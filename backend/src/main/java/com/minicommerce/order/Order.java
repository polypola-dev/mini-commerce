package com.minicommerce.order;

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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(String id, String customerId, List<OrderLineDraft> lineDrafts) {
        this.id = id;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
        this.totalAmount = lineDrafts.stream()
                .map(OrderLineDraft::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.lines = lineDrafts.stream()
                .map(draft -> new OrderLine(this, draft.productId(), draft.productName(), draft.unitPrice(), draft.quantity()))
                .toList();
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void markPaid() {
        if (status == OrderStatus.PENDING_PAYMENT) {
            status = OrderStatus.PAID;
        }
    }
}
