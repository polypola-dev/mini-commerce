package com.minicommerce.catalog;

import java.math.BigDecimal;

/** catalog 컨텍스트 외부(예: order)에 노출하는 조회 전용 공개 API. */
public interface ProductReader {
    record ProductInfo(String id, String name, BigDecimal price) {}
    record OptionInfo(BigDecimal additionalPrice, String optionValue) {}

    ProductInfo findProduct(String productId);
    OptionInfo findOption(String optionId);
}
