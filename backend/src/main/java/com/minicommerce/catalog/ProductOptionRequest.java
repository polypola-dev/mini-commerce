package com.minicommerce.catalog;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ProductOptionRequest(
        @NotBlank String optionGroupName,
        @NotBlank String optionValue,
        BigDecimal additionalPrice
) {}
