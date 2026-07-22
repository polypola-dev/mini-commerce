package com.minicommerce.catalog;

import java.math.BigDecimal;

public record ProductOptionResponse(
        String id,
        String optionGroupName,
        String optionValue,
        BigDecimal additionalPrice
) {
    static ProductOptionResponse from(ProductOption option) {
        return new ProductOptionResponse(
                option.getId().toString(),
                option.getOptionGroupName(),
                option.getOptionValue(),
                option.getAdditionalPrice()
        );
    }
}
