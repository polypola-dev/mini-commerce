package com.minicommerce.order.adapter.out.catalog;

import com.minicommerce.catalog.Product;
import com.minicommerce.catalog.ProductOption;
import com.minicommerce.catalog.ProductOptionRepository;
import com.minicommerce.catalog.ProductRepository;
import com.minicommerce.order.application.port.out.ProductQueryPort;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class CatalogProductAdapter implements ProductQueryPort {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    public CatalogProductAdapter(ProductRepository productRepository, ProductOptionRepository productOptionRepository) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
    }

    @Override
    public ProductInfo findProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        return new ProductInfo(product.getId(), product.getName(), product.getPrice());
    }

    @Override
    public OptionInfo findOption(String optionId) {
        ProductOption option = productOptionRepository.findById(optionId)
                .orElseThrow(() -> new EntityNotFoundException("Product option not found: " + optionId));
        return new OptionInfo(option.getAdditionalPrice(), option.getOptionValue());
    }
}
