package com.minicommerce.catalog;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        long availableStock,
        String imageUrl
) {
    static ProductResponse from(Product product, long availableStock) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                availableStock,
                product.getImageUrl()
        );
    }
}
