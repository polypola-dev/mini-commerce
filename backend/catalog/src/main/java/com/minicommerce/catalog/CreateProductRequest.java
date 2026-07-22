package com.minicommerce.catalog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @Positive BigDecimal price,
        @Min(0) long stock,
        @NotBlank String imageUrl,
        @NotBlank String sku,
        List<ProductOptionRequest> options
) {}
