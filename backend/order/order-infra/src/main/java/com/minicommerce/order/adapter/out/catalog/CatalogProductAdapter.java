package com.minicommerce.order.adapter.out.catalog;

import com.minicommerce.catalog.ProductReader;
import com.minicommerce.order.application.port.out.ProductQueryPort;
import org.springframework.stereotype.Component;

@Component
public class CatalogProductAdapter implements ProductQueryPort {

    private final ProductReader productReader;

    public CatalogProductAdapter(ProductReader productReader) {
        this.productReader = productReader;
    }

    @Override
    public ProductInfo findProduct(String productId) {
        ProductReader.ProductInfo info = productReader.findProduct(productId);
        return new ProductInfo(info.id(), info.name(), info.price());
    }

    @Override
    public OptionInfo findOption(String optionId) {
        ProductReader.OptionInfo info = productReader.findOption(optionId);
        return new OptionInfo(info.additionalPrice(), info.optionValue());
    }
}
