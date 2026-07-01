package com.minicommerce.order.application.port.out;

import java.math.BigDecimal;

public interface ProductQueryPort {
    record ProductInfo(String id, String name, BigDecimal price) {}
    record OptionInfo(BigDecimal additionalPrice, String optionValue) {}

    ProductInfo findProduct(String productId);
    OptionInfo findOption(String optionId);
}
