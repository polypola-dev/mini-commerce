package com.minicommerce.order.domain;

import com.minicommerce.order.domain.Order;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private long quantity;
    private String selectedOptionValue;

    protected OrderLine() {
    }

    public OrderLine(Order order, String productId, String productName, BigDecimal unitPrice, long quantity, String selectedOptionValue) {
        this.order = order;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.selectedOptionValue = selectedOptionValue;
    }
}
