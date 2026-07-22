package com.minicommerce.catalog;

import com.minicommerce.global.PageResult;
import com.minicommerce.shared.UuidV7;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/products")
@Transactional
public class ProductAdminController {

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final InventoryClient inventoryClient;

    public ProductAdminController(ProductRepository productRepository,
                                  ProductOptionRepository productOptionRepository,
                                  InventoryClient inventoryClient) {
        this.productRepository = productRepository;
        this.productOptionRepository = productOptionRepository;
        this.inventoryClient = inventoryClient;
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
        List<Product> products = productPage.getContent();
        Map<String, Long> stocks = inventoryClient.availableStocks(
                products.stream().map(p -> p.getId().toString()).toList());
        List<ProductResponse> content = products.stream()
                .map(p -> ProductResponse.from(
                        p,
                        stocks.getOrDefault(p.getId().toString(), 0L),
                        productOptionRepository.findByProductId(p.getId())))
                .toList();
        return new PageResult<>(content, productPage.getTotalElements(),
                productPage.getTotalPages(), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 SKU입니다: " + request.sku());
        }
        UUID id = UuidV7.randomUUID();
        Product product = new Product(id, request.name(), request.description(),
                request.price(), request.stock(), request.imageUrl(), request.sku());
        productRepository.save(product);
        // 재고 초기화는 상품 생성 시점에만 명시적으로 수행한다(조회는 순수 read-only, GH #6).
        // 여기서 seed하지 않으면 최초 조회 시 order-api가 재고 미존재를 default=0으로 간주해
        // 품절로 표시한다 — Redis에 값을 쓰지는 않으니 고착되지는 않지만, 실제 재고와 다르게
        // 보이는 창구가 생기므로 생성 시점에 반드시 seed한다.
        inventoryClient.setStock(id.toString(), request.stock());
        List<ProductOption> savedOptions = saveOptions(id, request.options());
        return ProductResponse.from(product, request.stock(), savedOptions);
    }

    @PutMapping("/{id}")
    ProductResponse update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest request) {
        UUID productId = UUID.fromString(id);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        // SKU를 다른 상품이 이미 쓰고 있으면 거부(자기 자신 제외).
        if (productRepository.existsBySkuAndIdNot(request.sku(), productId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 SKU입니다: " + request.sku());
        }
        product.update(request.name(), request.description(), request.price(),
                request.stock(), request.imageUrl(), request.sku());
        productRepository.save(product);
        inventoryClient.setStock(id, request.stock());
        productOptionRepository.deleteByProductId(productId);
        List<ProductOption> savedOptions = saveOptions(productId, request.options());
        return ProductResponse.from(
                product,
                inventoryClient.availableStock(product.getId().toString(), request.stock()),
                savedOptions);
    }

    private List<ProductOption> saveOptions(UUID productId, List<ProductOptionRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        List<ProductOption> options = requests.stream()
                .map(o -> new ProductOption(
                        UuidV7.randomUUID(),
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
        Product product = productRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.deactivate();
        productRepository.save(product);
    }

    @PatchMapping("/{id}/activate")
    ProductResponse activate(@PathVariable String id) {
        Product product = productRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.activate();
        productRepository.save(product);
        return ProductResponse.from(
                product,
                inventoryClient.availableStock(product.getId().toString(), product.getStock()),
                productOptionRepository.findByProductId(product.getId()));
    }
}
