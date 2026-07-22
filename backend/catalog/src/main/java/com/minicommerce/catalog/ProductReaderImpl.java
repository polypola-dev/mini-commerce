package com.minicommerce.catalog;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ProductReaderImpl implements ProductReader {
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    ProductReaderImpl(ProductRepository productRepository, ProductOptionRepository productOptionRepository) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
    }

    @Override
    public ProductInfo findProduct(String productId) {
        Product product = productRepository.findById(UUID.fromString(productId))
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
        return new ProductInfo(product.getId().toString(), product.getName(), product.getPrice());
    }

    @Override
    public OptionInfo findOption(String optionId) {
        ProductOption option = productOptionRepository.findById(UUID.fromString(optionId))
                .orElseThrow(() -> new EntityNotFoundException("Product option not found: " + optionId));
        return new OptionInfo(option.getAdditionalPrice(), option.getOptionValue());
    }
}
