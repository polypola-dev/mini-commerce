package com.minicommerce.catalog;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "product_options")
public class ProductOption {
    @Id
    private String id;
    private String productId;
    private String optionGroupName; // e.g. "색상", "모델"
    private String optionValue;     // e.g. "블랙", "Standard"
    private BigDecimal additionalPrice; // 0 이상

    protected ProductOption() {
    }

    public ProductOption(String id, String productId, String optionGroupName, String optionValue, BigDecimal additionalPrice) {
        this.id = id;
        this.productId = productId;
        this.optionGroupName = optionGroupName;
        this.optionValue = optionValue;
        this.additionalPrice = additionalPrice;
    }

    public String getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getOptionGroupName() {
        return optionGroupName;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public BigDecimal getAdditionalPrice() {
        return additionalPrice;
    }
}
