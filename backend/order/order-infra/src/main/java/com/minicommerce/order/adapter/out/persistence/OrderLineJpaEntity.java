package com.minicommerce.order.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/** 주문 라인의 JPA 영속성 모델. */
@Entity
@Table(name = "order_lines")
class OrderLineJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private OrderJpaEntity order;

    private UUID productId;
    private String productName;
    private BigDecimal unitPrice;
    private long quantity;
    private String selectedOptionValue;

    protected OrderLineJpaEntity() {
    }

    OrderLineJpaEntity(Long id, UUID productId, String productName, BigDecimal unitPrice, long quantity, String selectedOptionValue) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.selectedOptionValue = selectedOptionValue;
    }

    void assignOrder(OrderJpaEntity order) {
        this.order = order;
    }

    Long getId() { return id; }
    UUID getProductId() { return productId; }
    String getProductName() { return productName; }
    BigDecimal getUnitPrice() { return unitPrice; }
    long getQuantity() { return quantity; }
    String getSelectedOptionValue() { return selectedOptionValue; }
}
