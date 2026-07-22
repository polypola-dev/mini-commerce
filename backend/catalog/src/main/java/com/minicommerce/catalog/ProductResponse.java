package com.minicommerce.catalog;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        long availableStock,
        String imageUrl,
        String sku,
        boolean active,
        List<ProductOptionResponse> options
) {
    static ProductResponse from(Product product, long availableStock, List<ProductOption> options) {
        return new ProductResponse(
                product.getId().toString(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                availableStock,
                product.getImageUrl(),
                product.getSku(),
                product.isActive(),
                options.stream().map(ProductOptionResponse::from).toList()
        );
    }
}
