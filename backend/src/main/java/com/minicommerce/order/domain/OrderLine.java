package com.minicommerce.order.domain;

import java.math.BigDecimal;

/**
 * 주문 라인. 순수 POJO. id는 영속성 식별자로, 신규 생성 시 null이며 저장 후/복원 시 채워진다.
 */
public class OrderLine {
    private final Long id;
    private final String productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private final long quantity;
    private final String selectedOptionValue;

    public OrderLine(String productId, String productName, BigDecimal unitPrice, long quantity, String selectedOptionValue) {
        this(null, productId, productName, unitPrice, quantity, selectedOptionValue);
    }

    private OrderLine(Long id, String productId, String productName, BigDecimal unitPrice, long quantity, String selectedOptionValue) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.selectedOptionValue = selectedOptionValue;
    }

    /** 영속성에서 복원할 때만 사용 (Mapper 전용). */
    public static OrderLine reconstitute(Long id, String productId, String productName, BigDecimal unitPrice, long quantity, String selectedOptionValue) {
        return new OrderLine(id, productId, productName, unitPrice, quantity, selectedOptionValue);
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public long getQuantity() { return quantity; }
    public String getSelectedOptionValue() { return selectedOptionValue; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
