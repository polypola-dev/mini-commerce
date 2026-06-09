package com.minicommerce.catalog;

import com.minicommerce.inventory.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@Transactional
public class ProductAdminController {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final InventoryService inventoryService;

    public ProductAdminController(ProductRepository productRepository,
                                  ProductOptionRepository productOptionRepository,
                                  InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<ProductResponse> listAll() {
        return productRepository.findAll().stream()
                .map(p -> ProductResponse.from(
                        p,
                        inventoryService.availableStock(p.getId(), p.getStock()),
                        productOptionRepository.findByProductId(p.getId())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        String id = UUID.randomUUID().toString();
        Product product = new Product(id, request.name(), request.description(),
                request.price(), request.stock(), request.imageUrl());
        return ProductResponse.from(
                productRepository.save(product),
                request.stock(),
                List.of());
    }

    @PutMapping("/{id}")
    ProductResponse update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.update(request.name(), request.description(), request.price(),
                request.stock(), request.imageUrl());
        return ProductResponse.from(
                productRepository.save(product),
                inventoryService.availableStock(product.getId(), request.stock()),
                productOptionRepository.findByProductId(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.deactivate();
        productRepository.save(product);
    }
}
