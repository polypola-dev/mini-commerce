package com.minicommerce.catalog;

import com.minicommerce.global.PageResult;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final InventoryClient inventoryClient;

    public ProductController(ProductRepository productRepository, ProductOptionRepository productOptionRepository, InventoryClient inventoryClient) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.inventoryClient = inventoryClient;
    }

    @GetMapping
    PageResult<ProductResponse> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String qParam = (q != null && !q.isBlank()) ? q : null;
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> productPage = productRepository.findWithFilters(true, qParam, pageable);

        List<Product> products = productPage.getContent();
        Map<String, Long> stocks = inventoryClient.availableStocks(
                products.stream().map(p -> p.getId().toString()).toList());
        List<ProductResponse> content = products.stream()
                .map(product -> ProductResponse.from(
                        product,
                        stocks.getOrDefault(product.getId().toString(), 0L),
                        productOptionRepository.findByProductId(product.getId())))
                .toList();

        return new PageResult<>(content, productPage.getTotalElements(),
                productPage.getTotalPages(), page, size);
    }

    @GetMapping("/{id}")
    ProductResponse getProduct(@PathVariable String id) {
        Product product = productRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        return ProductResponse.from(
                product,
                inventoryClient.availableStock(product.getId().toString(), product.getStock()),
                productOptionRepository.findByProductId(product.getId()));
    }
}
