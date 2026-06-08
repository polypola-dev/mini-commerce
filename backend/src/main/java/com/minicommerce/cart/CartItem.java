package com.minicommerce.cart;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cart_items")
public class CartItem {
    @Id
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    private String productId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;
    private String selectedOptionId;
    private String selectedOptionValue;
    private Instant addedAt;

    protected CartItem() {
    }

    public CartItem(String id, Cart cart, String productId, String productName, BigDecimal unitPrice, int quantity) {
        this(id, cart, productId, productName, unitPrice, quantity, null, null);
    }

    public CartItem(String id, Cart cart, String productId, String productName, BigDecimal unitPrice, int quantity, String selectedOptionId, String selectedOptionValue) {
        this.id = id;
        this.cart = cart;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.selectedOptionId = selectedOptionId;
        this.selectedOptionValue = selectedOptionValue;
        this.addedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public String getSelectedOptionId() { return selectedOptionId; }
    public String getSelectedOptionValue() { return selectedOptionValue; }
    public Instant getAddedAt() { return addedAt; }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
