package com.minicommerce.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "items are required")
        List<@Valid CreateOrderItemRequest> items
) {
    public record CreateOrderItemRequest(
            @NotBlank(message = "productId is required")
            String productId,

            @Min(value = 1, message = "quantity must be greater than zero")
            long quantity,

            String selectedOptionId
    ) {
    }
}
