package com.minicommerce.catalog;

import com.minicommerce.global.PageResult;
import com.minicommerce.inventory.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
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
    private final InventoryService inventoryService;

    public ProductController(ProductRepository productRepository, ProductOptionRepository productOptionRepository, InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    PageResult<ProductResponse> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String qParam = (q != null && !q.isBlank()) ? q : null;
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> productPage = productRepository.findWithFilters(true, qParam, pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(product -> ProductResponse.from(
                        product,
                        inventoryService.availableStock(product.getId(), product.getStock()),
                        productOptionRepository.findByProductId(product.getId())))
                .toList();

        return new PageResult<>(content, productPage.getTotalElements(),
                productPage.getTotalPages(), page, size);
    }

    @GetMapping("/{id}")
    ProductResponse getProduct(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        return ProductResponse.from(
                product,
                inventoryService.availableStock(product.getId(), product.getStock()),
                productOptionRepository.findByProductId(product.getId()));
    }
}
