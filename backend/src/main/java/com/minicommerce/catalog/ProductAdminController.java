package com.minicommerce.catalog;

import com.minicommerce.global.PageResult;
import com.minicommerce.inventory.InventoryService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    PageResult<ProductResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active) {
        String qParam = (q != null && !q.isBlank()) ? q : null;
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Product> productPage = productRepository.findWithFilters(active, qParam, pageable);
        List<ProductResponse> content = productPage.getContent().stream()
                .map(p -> ProductResponse.from(
                        p,
                        inventoryService.availableStock(p.getId(), p.getStock()),
                        productOptionRepository.findByProductId(p.getId())))
                .toList();
        return new PageResult<>(content, productPage.getTotalElements(),
                productPage.getTotalPages(), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        String id = UUID.randomUUID().toString();
        Product product = new Product(id, request.name(), request.description(),
                request.price(), request.stock(), request.imageUrl());
        productRepository.save(product);
        List<ProductOption> savedOptions = saveOptions(id, request.options());
        return ProductResponse.from(product, request.stock(), savedOptions);
    }

    @PutMapping("/{id}")
    ProductResponse update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.update(request.name(), request.description(), request.price(),
                request.stock(), request.imageUrl());
        productRepository.save(product);
        inventoryService.setStock(id, request.stock());
        productOptionRepository.deleteByProductId(id);
        List<ProductOption> savedOptions = saveOptions(id, request.options());
        return ProductResponse.from(
                product,
                inventoryService.availableStock(product.getId(), request.stock()),
                savedOptions);
    }

    private List<ProductOption> saveOptions(String productId, List<ProductOptionRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        List<ProductOption> options = requests.stream()
                .map(o -> new ProductOption(
                        UUID.randomUUID().toString(),
                        productId,
                        o.optionGroupName(),
                        o.optionValue(),
                        o.additionalPrice() != null ? o.additionalPrice() : BigDecimal.ZERO))
                .toList();
        productOptionRepository.saveAll(options);
        return options;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.deactivate();
        productRepository.save(product);
    }

    @PatchMapping("/{id}/activate")
    ProductResponse activate(@PathVariable String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.activate();
        productRepository.save(product);
        return ProductResponse.from(
                product,
                inventoryService.availableStock(product.getId(), product.getStock()),
                productOptionRepository.findByProductId(id));
    }
}
